ALTER TABLE article_candidates
    MODIFY COLUMN category_hint VARCHAR(500) NULL;

UPDATE blog_sources
SET site_url = 'https://stripe.dev/blog/topic/engineering',
    feed_url = NULL,
    collection_method = 'HTML_SCRAPING',
    updated_at = NOW(6)
WHERE source_key = 'stripe';
