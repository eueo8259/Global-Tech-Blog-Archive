UPDATE blog_sources
SET feed_url = NULL,
    collection_method = 'HTML_SCRAPING',
    updated_at = NOW(6)
WHERE source_key = 'stripe';
