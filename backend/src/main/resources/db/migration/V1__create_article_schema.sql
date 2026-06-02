CREATE TABLE blog_sources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_key VARCHAR(50) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    site_url VARCHAR(500) NOT NULL,
    feed_url VARCHAR(500),
    collection_method VARCHAR(30) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_collected_at DATETIME(6),
    last_error_at DATETIME(6),
    last_error_msg VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_blog_sources_company_key (company_key)
);

CREATE TABLE articles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    original_url VARCHAR(2000) NOT NULL,
    normalized_url VARCHAR(2000) NOT NULL,
    normalized_url_hash VARCHAR(64) NOT NULL,
    category VARCHAR(30) NOT NULL,
    published_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_articles_source FOREIGN KEY (source_id) REFERENCES blog_sources (id),
    UNIQUE KEY uq_source_normalized_url_hash (source_id, normalized_url_hash),
    INDEX idx_category_published (category, published_at),
    INDEX idx_published (published_at)
);
