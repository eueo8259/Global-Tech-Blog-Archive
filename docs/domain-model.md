# Domain Model

## 1. Domain Overview

Global Tech Blog Archive collects article metadata from selected global company engineering blogs and exposes the articles through category-filtered lists.

The MVP domain is centered on collected articles and the blog sources they come from.

The MVP stores article metadata only. It does not store full article bodies, users, likes, saved articles, personalized feeds, search indexes, or article tags.

## 2. Ubiquitous Language

| Term | Meaning |
| --- | --- |
| Article | A single external engineering blog article collected from a source. |
| Company | The organization that owns or publishes the engineering blog. |
| Source | A configured company blog endpoint used by the collector. |
| Category | The single primary classification assigned to an article. |
| Article URL | The external article URL provided by the source and returned to users. |
| Article URL Hash | SHA-256 hash of the article URL used for database uniqueness. |
| Collection Method | The method used to collect from a source: `RSS`, `ATOM`, `SITEMAP`, or `HTML_SCRAPING`. |

## 3. Entities

### Article

`Article` is the main content entity shown to users.

Database table: `articles`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| company_id | BIGINT | yes | Foreign key to `companies.id` |
| title | VARCHAR(500) | yes | User-facing title. AI-approved article rows store the translated Korean title. |
| summary | TEXT | no | Legacy DB column. It is not mapped by the backend `Article` entity and is not exposed by the public article API. |
| article_url | VARCHAR(2000) | yes | Source-provided user-facing article URL and deduplication base |
| article_url_hash | VARCHAR(64) | yes | SHA-256 hash of `article_url` used for the unique constraint |
| category | VARCHAR(30) | yes | AI decision category for approved articles. `ELSE` is not stored in `articles` for AI-reviewed rows. |
| published_at | DATETIME | yes | Source publication time; use collection time when source publication time is missing |
| created_at | DATETIME | yes | Row creation time; also the first collection time |
| updated_at | DATETIME | yes | Row update time |

Required constraints and indexes:

```sql
CONSTRAINT fk_articles_company FOREIGN KEY (company_id) REFERENCES companies(id);
UNIQUE KEY uq_company_article_url_hash (company_id, article_url_hash);
INDEX idx_category_published (category, published_at);
INDEX idx_published (published_at);
```

### Company

`Company` is the publishing organization represented by a stable key and display name.

Database table: `companies`

Required fields:

```text
company_key
company_name
```

### Category

`Category` is a Java enum assigned by the classifier.

Valid stored values:

```text
FRONTEND
BACKEND
DEVOPS
ARCHITECTURE
AI
ELSE
```

`ALL` is a UI/API filter option only. It must not be stored in the database.

### Article AI Decision

`ArticleAiDecision` stores the AI review result for a collected candidate.

Database table: `article_ai_decisions`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| company_id | BIGINT | yes | Foreign key to `companies.id` |
| article_url_hash | VARCHAR(64) | yes | SHA-256 hash used with company and prompt version as the AI decision identity |
| article_url | VARCHAR(2000) | yes | Source-provided user-facing article URL |
| original_title | VARCHAR(500) | yes | Title seen during crawl |
| translated_title | VARCHAR(500) | yes | Korean title returned by AI |
| category | VARCHAR(30) | yes | AI category: `FRONTEND`, `BACKEND`, `DEVOPS`, `ARCHITECTURE`, `AI`, or `ELSE` |
| save_target | TINYINT(1) | yes | Whether this candidate should be saved to `articles` |
| model | VARCHAR(100) | yes | Model used for the decision |
| prompt_version | VARCHAR(50) | yes | Prompt version used for re-review control |
| created_at | DATETIME | yes | Row creation time |
| updated_at | DATETIME | yes | Row update time |

Required constraints:

```sql
UNIQUE KEY uq_article_ai_decision_company_url_hash_prompt (company_id, article_url_hash, prompt_version);
```

### Article Candidate

`ArticleCandidate` is the durable work item between source collection and AI review.

Database table: `article_candidates`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| company_id | BIGINT | yes | Foreign key to `companies.id` |
| source_id | BIGINT | yes | Foreign key to `blog_sources.id` |
| article_url | VARCHAR(2000) | yes | Normalized source URL |
| article_url_hash | VARCHAR(64) | yes | Candidate business identity with company |
| original_title | VARCHAR(500) | yes | Title collected from the source |
| short_context | TEXT | no | Short AI review context |
| category_hint | VARCHAR(100) | no | Optional source category hint |
| published_at | DATETIME | no | Source publication time |
| status | VARCHAR(30) | yes | Current candidate processing state |
| attempt_count | INT | yes | Number of AI processing claims |
| next_retry_at | DATETIME | no | Earliest retry claim time |
| processing_started_at | DATETIME | no | Claim time and stale-work reference |
| last_error_code | VARCHAR(100) | no | Last processing failure code |
| last_error_message | VARCHAR(500) | no | Truncated diagnostic message |
| processing_prompt_version | VARCHAR(50) | no | Prompt version used by the current/latest claim |
| created_at | DATETIME | yes | First discovery time |
| updated_at | DATETIME | yes | Last state change time |

Required constraints and indexes:

```sql
UNIQUE KEY uq_article_candidates_company_url_hash (company_id, article_url_hash);
INDEX idx_article_candidates_status_retry (status, next_retry_at);
INDEX idx_article_candidates_status_processing (status, processing_started_at);
```

Candidate state transitions:

```text
NEW -> AI_PROCESSING -> AI_APPROVED | AI_REJECTED
AI_PROCESSING -> AI_RETRY_WAITING -> AI_PROCESSING
AI_PROCESSING -> AI_FAILED
```

`PREVIOUSLY_APPROVED`, `PREVIOUSLY_REJECTED`, and `DUPLICATE` preserve cached
decision and existing-article behavior without another OpenAI call.

### Source

`Source` is a configured company blog endpoint used by the collector.

Database table: `blog_sources`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| company_id | BIGINT | yes | Foreign key to `companies.id` |
| source_key | VARCHAR(80) | yes | Stable lowercase source key, unique |
| source_name | VARCHAR(100) | yes | Display name for this source |
| site_url | VARCHAR(500) | yes | Blog home or engineering page URL |
| feed_url | VARCHAR(500) | no | RSS/Atom URL; null for HTML scraping |
| collection_method | VARCHAR(30) | yes | Java enum value: `RSS`, `ATOM`, `SITEMAP`, or `HTML_SCRAPING` |
| enabled | TINYINT(1) | yes | `1` means collect this source; `0` means skip |
| last_collected_at | DATETIME | no | Last successful collection time |
| last_error_at | DATETIME | no | Last failed collection time |
| last_error_msg | VARCHAR(500) | no | Last failure reason |
| created_at | DATETIME | yes | Row creation time |
| updated_at | DATETIME | yes | Row update time |

Required constraints:

```sql
UNIQUE KEY uq_blog_sources_source_key (source_key);
```

Field rules:

- `feed_url` is required when `collection_method` is `RSS`, `ATOM`, or `SITEMAP`.
- `feed_url` must be null when `collection_method` is `HTML_SCRAPING`.
- `enabled` defaults to `1`.

## 4. Aggregates

### Article Aggregate

`Article` is the aggregate root for collected article metadata.

The aggregate includes:

- article identity
- company reference
- translated title
- article URL
- article URL hash
- category
- publication and persistence timestamps

The aggregate does not include:

- summary
- full article body
- article tags
- user-specific state
- collection run history

`Company` is referenced by `company_id`. Updating source configuration must not require updating existing articles.

## 5. Business Rules

- The collector runs automatically on a schedule by default.
- The collector reads only `blog_sources` rows where `enabled = 1`.
- Recent collection keeps up to 20 candidates per source from the last two days and excludes candidates without a publication time.
- Initial collection keeps up to 20 candidates per source without a date window; dated candidates are ordered newest first and undated candidates fill remaining positions afterward.
- Each source collection is committed independently so one source failure does not roll back successful results from other sources in the same run.
- RSS and Atom collection use `feed_url`.
- Sitemap collection uses `feed_url`.
- HTML scraping uses `site_url`.
- Collected candidates are committed as durable work before an OpenAI call.
- Article category assignment is handled by AI decision before Article persistence.
- Each article has exactly one stored category.
- The crawler stores the source-provided URL as `article_url`.
- `article_url` is both the user-facing article link and the deduplication base.
- Query parameters are preserved in the MVP.
- URL normalization is intentionally minimal: resolve relative URLs, normalize path syntax, lowercase scheme/host, decode escaped `&`, trim whitespace, and drop fragments.
- `article_url_hash` is the SHA-256 hash of `article_url`.
- Duplicate article rows are blocked by `UNIQUE(company_id, article_url_hash)` as the final database safety net.
- AI re-review and crawl pre-processing use `article_ai_decisions`, not `articles`, as the primary cache/gate.
- `article_ai_decisions.save_target=true` and `category != ELSE` allows saving to `articles`.
- `article_ai_decisions.save_target=false` or `category=ELSE` prevents saving to `articles`.
- Previously approved decisions may save article rows without calling AI again.
- Previously rejected decisions do not call AI again and do not save article rows.
- OpenAI calls run without an active database transaction.
- AI decisions, approved Articles, and final candidate status are committed in one short transaction.
- Timeout, HTTP 429, and temporary 5xx failures move candidates to `AI_RETRY_WAITING` until the maximum attempt count is reached.
- Stale `AI_PROCESSING` candidates move to `AI_RETRY_WAITING`; a claim timestamp prevents an older worker result from overwriting a newer claim.
- AI failures remain in `article_candidates` as `AI_FAILED`; no decision row is created because no valid AI decision exists.
- If the prompt changes, increment `prompt_version` to allow re-review.
- If the source does not provide a publication time, set `published_at` to the collection time.
- `created_at` represents the first time the article row was stored.
- Source collection success updates `last_collected_at`.
- Source collection failure updates `last_error_at` and `last_error_msg`.
- Collection run logs store discovered candidates and their decision status.

## 6. Slack Subscription Model

Slack subscriptions use a workspace-level bot token instead of channel-specific
webhook URLs.

### Slack Workspace

`SlackWorkspace` is the Slack installation/workspace that owns the encrypted bot
token used for future channel messages.

Database table: `slack_workspaces`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| slack_team_id | VARCHAR(50) | yes | Slack workspace/team id |
| slack_team_name | VARCHAR(100) | yes | Slack workspace display name |
| encrypted_bot_token | VARCHAR(1000) | yes | AES-GCM encrypted Slack bot token |
| bot_user_id | VARCHAR(50) | no | Slack bot user id returned by OAuth |
| scope | VARCHAR(500) | no | Granted Slack OAuth scopes |
| installed_at | DATETIME | yes | Installation or latest reinstall time |
| created_at | DATETIME | yes | Row creation time |
| updated_at | DATETIME | yes | Row update time |

Required constraints:

```sql
UNIQUE KEY uq_slack_workspaces_team_id (slack_team_id);
```

### Slack Channel

`SlackChannel` is a Slack channel inside one workspace. It does not store a bot
token directly; messages use the parent workspace's bot token.

Database table: `slack_channels`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| slack_workspace_id | BIGINT | yes | Foreign key to `slack_workspaces.id` |
| slack_channel_id | VARCHAR(50) | yes | Slack channel id |
| slack_channel_name | VARCHAR(100) | yes | Slack channel display name |
| created_at | DATETIME | yes | Row creation time |
| updated_at | DATETIME | yes | Row update time |

Required constraints:

```sql
UNIQUE KEY uq_slack_channels_workspace_channel (slack_workspace_id, slack_channel_id);
```

### Slack Channel Subscription

`SlackChannelSubscription` connects a Slack channel to a company. It represents
"this Slack channel subscribes to this company's new articles."

Database table: `slack_channel_subscriptions`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| slack_channel_id | BIGINT | yes | Foreign key to `slack_channels.id` |
| company_id | BIGINT | yes | Foreign key to `companies.id` |
| created_at | DATETIME | yes | Row creation time |

Required constraints:

```sql
UNIQUE KEY uq_slack_channel_subscriptions_channel_company (slack_channel_id, company_id);
INDEX idx_slack_channel_subscriptions_company (company_id);
```

Delivery lookup direction:

```text
09:00 Daily Digest batch
-> find subscribed Slack channels
-> find articles created in each channel's delivery window for subscribed companies
-> group articles by company
-> decrypt the parent workspace bot token
-> send one message per channel and delivery date
```

### Slack Delivery

`SlackDelivery` records the business result of one channel's Daily Digest. Spring
Batch metadata records Job and Step execution only and does not replace this
delivery state.

Database table: `slack_deliveries`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| slack_channel_id | BIGINT | yes | Target channel foreign key |
| delivery_date | DATE | yes | Delivery date in the configured digest time zone |
| status | VARCHAR(30) | yes | `PENDING`, `PROCESSING`, `RETRY_WAITING`, `SENT`, or `FAILED` |
| attempt_count | INT | yes | Number of claimed send attempts |
| window_started_at | DATETIME | yes | Exclusive article creation lower bound |
| window_ended_at | DATETIME | yes | Inclusive article creation upper bound |
| processing_started_at | DATETIME | no | Used to recover stale processing claims |
| sent_at | DATETIME | no | Successful completion time |
| next_retry_at | DATETIME | no | Earliest time a retry may claim the delivery |
| last_error_code | VARCHAR(100) | no | Last Slack or internal error code |
| last_error_message | VARCHAR(500) | no | Truncated diagnostic message |
| slack_message_ts | VARCHAR(50) | no | Slack message timestamp returned by `chat.postMessage` |
| created_at | DATETIME | yes | Row creation time |
| updated_at | DATETIME | yes | Row update time |

Required constraint:

```sql
UNIQUE KEY uq_slack_deliveries_channel_date
    (slack_channel_id, delivery_date);
```

Delivery rules:

- The first window starts at the Slack channel creation time.
- Later windows start at the previous `SENT` delivery's `window_ended_at`.
- Article lookup uses `(window_started_at, window_ended_at]` and includes only
  companies subscribed at or before the article was stored.
- A channel receives at most one Slack API call for one Daily Digest attempt.
- `SENT` deliveries are never selected for retry.
- A stale `PROCESSING` delivery becomes `RETRY_WAITING`.
- Delivery items are not snapshotted in the MVP; retry re-queries the fixed
  delivery window using current subscription data.

Database schema and company/source reference data are managed by immutable
Flyway migrations. Locally collected articles and AI decisions may be promoted
once through the bootstrap archive process documented in
`backend/docs/bootstrap-deployment.md`; collection run logs are not promoted.

Candidate processing and observed decision statuses:

```text
NEW
AI_PROCESSING
AI_RETRY_WAITING
PREVIOUSLY_APPROVED
PREVIOUSLY_REJECTED
AI_APPROVED
AI_REJECTED
AI_FAILED
DUPLICATE
```
