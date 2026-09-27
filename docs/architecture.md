# Architecture

## 1. Overview

Global Tech Blog Archive collects engineering blog article metadata from selected global technology companies and exposes it through a Spring Boot API and React frontend.

## 2. System Components

- Frontend: React, TypeScript, Vite
- Backend API: Java 21, Spring Boot
- Collector: RSS/Atom-first collection with explicitly limited HTML list-page parsing
- AI reviewer: OpenAI-based selection, title translation, and single-category classification
- Database: MySQL
- Scheduled delivery: Spring Scheduler, DB delivery state, and Slack `chat.postMessage`
- External Sources: company engineering blogs defined in `article-source-strategy.md`

### Domain Ownership

Collector is responsible for:

- fetching article lists from external sources
- parsing RSS/Atom feeds
- parsing HTML list pages when allowed by the collection strategy
- extracting raw article metadata

Collector is not responsible for:

- assigning article categories
- formatting API responses
- frontend filtering behavior

Classifier is responsible for:

- assigning one primary category to each article
- applying the category rules defined in `article-source-strategy.md`

Classifier is not responsible for:

- fetching external articles
- parsing RSS/Atom or HTML
- storing articles
- formatting API responses

Storage is responsible for:

- persisting article metadata
- enforcing URL-based deduplication

Storage is not responsible for:

- fetching external articles
- assigning article categories
- deciding API response shape

API is responsible for:

- returning article data to the frontend
- filtering articles by category
- paginating article lists

API is not responsible for:

- fetching external articles
- parsing RSS/Atom or HTML
- assigning article categories

## 3. Collection Strategy

- Collect articles only from the companies defined in `article-source-strategy.md`.
- Use RSS or Atom feeds as the default collection method.
- HTML list-page parsing is allowed only when one of these conditions is true:
  - the source does not provide an RSS or Atom feed
  - the feed omits one or more MVP-required fields: title, original URL, published date, summary, or company/source identity
  - the feed contains only product/news entries while the source has a separate engineering article list page

## 4. Data Flow

```text
External Blog Sources
        |
        v
Collector
        |
        v
Persist Candidate (`NEW`)
        |
        v
AI Review Scheduler
        |
        v
Claim Candidate (`AI_PROCESSING`)
        |
        v
OpenAI API (outside transaction)
        |
        v
Persist Decision + Approved Article + Final Candidate Status
        |
        v
Spring Boot API
        |
        v
React Frontend
```

Article collection and AI review are separate scheduler flows. Collection ends
after the candidate and its immutable discovery history are committed. The AI
review scheduler claims persisted candidates in a short transaction, releases
the transaction before the external API call, and commits the decision, approved
article, and final candidate status together afterward. Retryable failures are
delayed in `AI_RETRY_WAITING`, while stale `AI_PROCESSING` claims are recovered
from `processing_started_at`.

Slack Daily Digest follows a separate scheduled read path:

```text
Stored Articles + Slack Subscriptions
        |
        v
Daily Digest Scheduler (09:00 Asia/Seoul)
        |
        v
Record Daily Run + Prepare `SlackDelivery`
        |
        v
Channel-level Daily Digest
        |
        v
Slack chat.postMessage
        |
        v
Definite result -> `SENT` / retry state
Ambiguous result -> `VERIFYING`
        |
        v
Slack conversations.history reconciliation
```

Article persistence does not create subscriber-specific delivery rows. The
The Daily Digest service records one `SlackDailyDigestRun` per delivery date,
prepares `SlackDelivery` rows only for channels that have articles in their
next delivery window, then sends each delivery independently. A completed date
is not executed again. After the cutoff time, the five-minute retry schedule
also calls the daily flow so a failed run, a persisted `RUNNING` run from a
terminated process, or a missed 09:00 schedule can catch up. If the daily run is
already complete, the schedule processes retryable `SlackDelivery` rows directly
without creating another daily run row.
Slack API calls run outside the JPA transaction; short independent transactions
claim work and record success or failure. The service currently runs as a single
application instance, and a process-local guard prevents the daily and retry
schedules from overlapping. Because a persisted `RUNNING` row can only outlive
a terminated process under this deployment model, it can restart immediately
when a later schedule acquires the guard.

Each `SlackDelivery` owns a stable UUID `delivery_key`. The dispatcher includes
it in Slack message metadata but never in visible message text. This metadata is
a correlation key for reconciliation, not a Slack-side idempotency key.

Definite failures such as rate limiting, authentication rejection, or channel
rejection follow the normal retry/failure rules. Ambiguous outcomes such as a
network timeout, HTTP 5xx, a failed success-response deserialization, or a failed
`SENT` database write move to `VERIFYING`. Stale `PROCESSING` work also moves to
`VERIFYING` while preserving the send-attempt timestamp.

The retry scheduler first queries due `VERIFYING` deliveries through
`conversations.history`, then dispatches ready `PENDING` and `RETRY_WAITING`
deliveries. A matching delivery key confirms `SENT`. Only three complete,
successful History scans that find no match allow resend; API failures never
count as absence. Permission failures remain in `VERIFYING` with a longer delay.
This provides recovery close to effectively-once delivery, not mathematical
Exactly Once. Delivery-level verification and dispatch failures are isolated so
remaining channels continue.

Operational Slack scope, metadata schema, and reinstall steps are documented in
[`slack-delivery-recovery.md`](slack-delivery-recovery.md).

Legacy Spring Batch metadata tables remain in the database during the migration
period even though the application no longer reads or writes them. Keeping the
tables allows rollback to the previous Batch-based release. Their removal is a
separate operational migration after the new delivery flow is stable and the
rollback window has closed.

## 5. Storage Policy

- Store article metadata: article URL, article URL hash, title, published date, row creation time, company, and category.
- The source-provided `article_url` is the user-facing article link and the deduplication base.
- Preserve query parameters in the MVP; avoid source-specific URL cleanup until there is a concrete duplicate problem.
- Store a SHA-256 `article_url_hash` for the database unique constraint.
- Each article has one primary category.
- `ALL` is a UI/API filter option, not a stored article category.
- Deduplicate articles by `company_id` and `article_url_hash`.
- Deduplicate candidate work by `company_id` and `article_url_hash`.
- Keep `article_collections` as immutable per-run discovery history; use
  `article_candidates` as the mutable AI work queue.
- Do not store a separate `collected_at`; `created_at` represents the first collection time.
- Do not store full article bodies in the MVP.

## 6. Search Strategy

- Initial browsing uses category filters and latest-first article lists.
- Search functionality is not part of the initial MVP.
- If search is added after the initial MVP, the first implementation must use database search over title and summary.
- Defer Elasticsearch, OpenSearch, vector search, and recommendation systems until after the MVP.

## 7. Architecture Constraints

- Add a company-specific parser only when the generic feed collector and generic list-page collector both fail to produce the MVP-required fields.
- Keep collection, classification, storage, and API responsibilities separate.
- Login, likes, saves, recommendation, and personalization are out of scope for the MVP.
