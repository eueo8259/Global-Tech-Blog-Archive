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
| Original URL | The URL found during collection and used as the user-facing article link. |
| Normalized URL | The normalized form of the original URL used for deduplication. |
| Collection Method | The method used to collect from a source: `RSS`, `ATOM`, or `HTML_SCRAPING`. |

## 3. Entities

### Article

`Article` is the main content entity shown to users.

Database table: `articles`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| source_id | BIGINT | yes | Foreign key to `blog_sources.id` |
| title | VARCHAR(500) | yes | Article title |
| summary | TEXT | no | Feed summary, page description, or extracted short description |
| original_url | VARCHAR(2000) | yes | User-facing source-of-truth URL |
| normalized_url | VARCHAR(2000) | yes | Deduplication URL generated before persistence |
| category | VARCHAR(30) | yes | Java enum value stored as a string |
| published_at | DATETIME | yes | Source publication time; use collection time when source publication time is missing |
| created_at | DATETIME | yes | Row creation time; also the first collection time |
| updated_at | DATETIME | yes | Row update time |

Required constraints and indexes:

```sql
CONSTRAINT fk_articles_source FOREIGN KEY (source_id) REFERENCES blog_sources(id);
UNIQUE KEY uq_source_normalized_url (source_id, normalized_url);
INDEX idx_category_published (category, published_at);
INDEX idx_published (published_at);
```

### Company

`Company` is the publishing organization represented by a stable key and display name.

There is no separate `companies` table in the MVP. Company fields are stored on `blog_sources`.

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

### Source

`Source` is a configured company blog endpoint used by the collector.

Database table: `blog_sources`

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| id | BIGINT | yes | Primary key |
| company_key | VARCHAR(50) | yes | Stable lowercase key, unique |
| company_name | VARCHAR(100) | yes | Display name |
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
UNIQUE KEY uq_blog_sources_company_key (company_key);
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
- source reference
- title and summary
- original URL and normalized URL
- category
- publication and persistence timestamps

The aggregate does not include:

- full article body
- article tags
- user-specific state
- collection run history

`Source` is referenced by `source_id`. Updating source configuration must not require updating existing articles.

## 5. Business Rules

- The collector runs automatically on a schedule by default.
- The collector reads only `blog_sources` rows where `enabled = 1`.
- RSS and Atom collection use `feed_url`.
- HTML scraping uses `site_url`.
- Article category assignment is handled by the classifier before persistence.
- Each article has exactly one stored category.
- The original URL is preserved and used as the article link returned to the frontend.
- The normalized URL is used only for deduplication.
- Duplicate articles are blocked by `UNIQUE(source_id, normalized_url)`.
- When a duplicate is detected, the article is skipped.
- The duplicate handling implementation may use `INSERT IGNORE`, `ON DUPLICATE KEY UPDATE`, or exception handling.
- If the source does not provide a publication time, set `published_at` to the collection time.
- `created_at` represents the first time the article row was stored.
- Source collection success updates `last_collected_at`.
- Source collection failure updates `last_error_at` and `last_error_msg`.
- Full collection history is not stored in the MVP.
