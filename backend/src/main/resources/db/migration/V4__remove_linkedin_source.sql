DELETE FROM article_collections
WHERE source_id IN (
    SELECT id FROM blog_sources WHERE source_key = 'linkedin'
);

DELETE FROM blog_sources
WHERE source_key = 'linkedin';

DELETE FROM articles
WHERE company_id IN (
    SELECT id FROM companies WHERE company_key = 'linkedin'
);

DELETE FROM article_ai_decisions
WHERE company_id IN (
    SELECT id FROM companies WHERE company_key = 'linkedin'
);

DELETE FROM companies
WHERE company_key = 'linkedin';
