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
| Article URL | The cleaned, user-facing external article URL used as the deduplication base. |
| Article URL Hash | SHA-256 hash of the article URL used for database uniqueness. |
| Collection Method | The method used to collect from a source: `RSS`, `ATOM`, or `HTML_SCRAPING`. |

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
| article_url | VARCHAR(2000) | yes | Cleaned user-facing article URL and deduplication base |
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
| article_url | VARCHAR(2000) | yes | Cleaned user-facing article URL |
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
| collection_method | VARCHAR(30) | yes | Java enum value: `RSS`, `ATOM`, or `HTML_SCRAPING` |
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

- `feed_url` is required when `collection_method` is `RSS` or `ATOM`.
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
- RSS and Atom collection use `feed_url`.
- HTML scraping uses `site_url`.
- Article category assignment is handled by AI decision before persistence.
- Each article has exactly one stored category.
- The crawler converts collected URLs into cleaned `article_url` values before persistence.
- `article_url` is both the user-facing article link and the deduplication base.
- Tracking parameters such as `utm_*`, `source`, `fbclid`, `gclid`, `mc_cid`, and `mc_eid` are removed.
- Meaningful query parameters such as `id` and `page` are preserved.
- `article_url_hash` is the SHA-256 hash of `article_url`.
- Duplicate article rows are blocked by `UNIQUE(company_id, article_url_hash)` as the final database safety net.
- AI re-review and crawl pre-processing use `article_ai_decisions`, not `articles`, as the primary cache/gate.
- `article_ai_decisions.save_target=true` and `category != ELSE` allows saving to `articles`.
- `article_ai_decisions.save_target=false` or `category=ELSE` prevents saving to `articles`.
- Previously approved decisions may save article rows without calling AI again.
- Previously rejected decisions do not call AI again and do not save article rows.
- AI failures are recorded in crawl logs as `AI_FAILED`; no decision row is created because no valid AI decision exists.
- If the prompt changes, increment `prompt_version` to allow re-review.
- If the source does not provide a publication time, set `published_at` to the collection time.
- `created_at` represents the first time the article row was stored.
- Source collection success updates `last_collected_at`.
- Source collection failure updates `last_error_at` and `last_error_msg`.
- Collection run logs store discovered candidates and their decision status.

Candidate decision statuses used in crawl logs:

```text
NEW
PREVIOUSLY_APPROVED
PREVIOUSLY_REJECTED
AI_APPROVED
AI_REJECTED
AI_FAILED
```
