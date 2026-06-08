# Architecture

## 1. Overview

Global Tech Blog Archive collects engineering blog article metadata from selected global technology companies and exposes it through a Spring Boot API and React frontend.

## 2. System Components

- Frontend: React, TypeScript, Vite
- Backend API: Java 21, Spring Boot
- Collector: RSS/Atom-first collection with explicitly limited HTML list-page parsing
- Classifier: keyword-based single-category classification
- Database: MySQL
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
Category Classifier
        |
        v
MySQL
        |
        v
Spring Boot API
        |
        v
React Frontend
```

## 5. Storage Policy

- Store article metadata: original URL, normalized URL, normalized URL hash, title, summary, published date, row creation time, company name, and category.
- The original article URL is the source of truth.
- Use normalized URL only for deduplication.
- Store a SHA-256 normalized URL hash for the database unique constraint.
- Each article has one primary category.
- `ALL` is a UI/API filter option, not a stored article category.
- Deduplicate articles by `company_id` and `normalized_url_hash`.
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
