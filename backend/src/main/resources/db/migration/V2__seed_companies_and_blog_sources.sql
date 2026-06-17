INSERT INTO companies (company_key, company_name, created_at, updated_at) VALUES
('openai', 'OpenAI', NOW(6), NOW(6)),
('anthropic', 'Anthropic', NOW(6), NOW(6)),
('netflix', 'Netflix', NOW(6), NOW(6)),
('figma', 'Figma', NOW(6), NOW(6)),
('meta', 'Meta', NOW(6), NOW(6)),
('uber', 'Uber', NOW(6), NOW(6)),
('airbnb', 'Airbnb', NOW(6), NOW(6)),
('pinterest', 'Pinterest', NOW(6), NOW(6)),
('stripe', 'Stripe', NOW(6), NOW(6)),
('cloudflare', 'Cloudflare', NOW(6), NOW(6)),
('github', 'GitHub', NOW(6), NOW(6)),
('linkedin', 'LinkedIn', NOW(6), NOW(6)),
('discord', 'Discord', NOW(6), NOW(6)),
('shopify', 'Shopify', NOW(6), NOW(6)),
('datadog', 'Datadog', NOW(6), NOW(6)),
('slack', 'Slack', NOW(6), NOW(6)),
('amazon-science', 'Amazon Science', NOW(6), NOW(6));

INSERT INTO blog_sources (
    company_id, source_key, source_name, site_url, feed_url,
    collection_method, enabled, created_at, updated_at
)
SELECT id, 'openai', 'OpenAI News', 'https://openai.com/news/', 'https://openai.com/news/rss.xml', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'openai'
UNION ALL
SELECT id, 'anthropic-engineering', 'Anthropic Engineering', 'https://www.anthropic.com/engineering', 'https://www.anthropic.com/sitemap.xml', 'SITEMAP', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'anthropic'
UNION ALL
SELECT id, 'claude-blog', 'Claude Blog', 'https://claude.com/blog', NULL, 'HTML_SCRAPING', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'anthropic'
UNION ALL
SELECT id, 'netflix', 'Netflix Tech Blog', 'https://netflixtechblog.com/', 'https://netflixtechblog.com/feed', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'netflix'
UNION ALL
SELECT id, 'figma', 'Figma Engineering Blog', 'https://www.figma.com/blog/engineering/', 'https://www.figma.com/blog/feed/atom.xml', 'ATOM', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'figma'
UNION ALL
SELECT id, 'meta', 'Meta Engineering', 'https://engineering.fb.com/', 'https://engineering.fb.com/feed/', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'meta'
UNION ALL
SELECT id, 'uber', 'Uber Engineering Blog', 'https://www.uber.com/blog/engineering', NULL, 'HTML_SCRAPING', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'uber'
UNION ALL
SELECT id, 'airbnb', 'Airbnb Engineering', 'https://medium.com/airbnb-engineering', 'https://medium.com/feed/airbnb-engineering', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'airbnb'
UNION ALL
SELECT id, 'pinterest', 'Pinterest Engineering', 'https://medium.com/pinterest-engineering', 'https://medium.com/feed/pinterest-engineering', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'pinterest'
UNION ALL
SELECT id, 'stripe', 'Stripe Engineering Blog', 'https://stripe.com/blog/engineering', 'https://stripe.com/blog/feed.rss', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'stripe'
UNION ALL
SELECT id, 'cloudflare', 'Cloudflare Blog', 'https://blog.cloudflare.com/', 'https://blog.cloudflare.com/tag/engineering/rss/', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'cloudflare'
UNION ALL
SELECT id, 'github', 'GitHub Engineering', 'https://github.blog/engineering/', 'https://github.blog/engineering/feed/', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'github'
UNION ALL
SELECT id, 'linkedin', 'LinkedIn Engineering Blog', 'https://engineering.linkedin.com/content/engineering/en-us/blog', NULL, 'HTML_SCRAPING', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'linkedin'
UNION ALL
SELECT id, 'discord', 'Discord Engineering', 'https://discord.com/category/engineering', NULL, 'HTML_SCRAPING', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'discord'
UNION ALL
SELECT id, 'shopify', 'Shopify Engineering', 'https://shopify.engineering/', 'https://shopify.engineering/sitemap.xml', 'SITEMAP', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'shopify'
UNION ALL
SELECT id, 'datadog', 'Datadog Engineering', 'https://www.datadoghq.com/blog/engineering/', 'https://www.datadoghq.com/blog/engineering/index.xml', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'datadog'
UNION ALL
SELECT id, 'slack', 'Slack Engineering', 'https://slack.engineering/', 'https://slack.engineering/feed/', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'slack'
UNION ALL
SELECT id, 'amazon-science', 'Amazon Science Blog', 'https://www.amazon.science/blog', 'https://www.amazon.science/index.rss', 'RSS', 1, NOW(6), NOW(6) FROM companies WHERE company_key = 'amazon-science';
