CREATE TABLE companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_key VARCHAR(50) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_companies_company_key UNIQUE (company_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE blog_sources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    source_key VARCHAR(80) NOT NULL,
    source_name VARCHAR(100) NOT NULL,
    site_url VARCHAR(500) NOT NULL,
    feed_url VARCHAR(500) NULL,
    collection_method VARCHAR(30) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_collected_at DATETIME(6) NULL,
    last_error_at DATETIME(6) NULL,
    last_error_msg VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_blog_sources_source_key UNIQUE (source_key),
    CONSTRAINT fk_blog_sources_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE articles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT NULL,
    article_url VARCHAR(2000) NOT NULL,
    article_url_hash VARCHAR(64) NOT NULL,
    category VARCHAR(30) NOT NULL,
    published_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_company_article_url_hash UNIQUE (company_id, article_url_hash),
    CONSTRAINT fk_articles_company FOREIGN KEY (company_id) REFERENCES companies (id),
    INDEX idx_category_published (category, published_at),
    INDEX idx_published (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE article_ai_decisions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    article_url_hash VARCHAR(64) NOT NULL,
    article_url VARCHAR(2000) NOT NULL,
    original_title VARCHAR(500) NOT NULL,
    translated_title VARCHAR(500) NOT NULL,
    category VARCHAR(30) NOT NULL,
    save_target TINYINT(1) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_article_ai_decision_company_url_hash_prompt
        UNIQUE (company_id, article_url_hash, prompt_version),
    CONSTRAINT fk_article_ai_decisions_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE article_collection_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    source_count INT NOT NULL,
    success_count INT NOT NULL,
    failure_count INT NOT NULL,
    discovered_count INT NOT NULL,
    duplicate_count INT NOT NULL,
    stored_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE article_collections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    company_key VARCHAR(50) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    original_title VARCHAR(500) NOT NULL,
    article_url VARCHAR(2000) NOT NULL,
    published_at DATETIME(6) NULL,
    short_context TEXT NULL,
    article_url_hash VARCHAR(64) NOT NULL,
    duplicate_article TINYINT(1) NOT NULL,
    decision_status VARCHAR(30) NOT NULL,
    validation_warnings VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_article_collections_run FOREIGN KEY (run_id) REFERENCES article_collection_runs (id),
    CONSTRAINT fk_article_collections_source FOREIGN KEY (source_id) REFERENCES blog_sources (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
