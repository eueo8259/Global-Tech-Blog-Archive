CREATE TABLE article_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    article_url VARCHAR(2000) NOT NULL,
    article_url_hash VARCHAR(64) NOT NULL,
    original_title VARCHAR(500) NOT NULL,
    short_context TEXT NULL,
    category_hint VARCHAR(100) NULL,
    published_at DATETIME(6) NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NULL,
    processing_started_at DATETIME(6) NULL,
    last_error_code VARCHAR(100) NULL,
    last_error_message VARCHAR(500) NULL,
    processing_prompt_version VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_article_candidates_company_url_hash UNIQUE (company_id, article_url_hash),
    CONSTRAINT fk_article_candidates_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_article_candidates_source FOREIGN KEY (source_id) REFERENCES blog_sources (id),
    INDEX idx_article_candidates_status_retry (status, next_retry_at),
    INDEX idx_article_candidates_status_processing (status, processing_started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO article_candidates (
    company_id,
    source_id,
    article_url,
    article_url_hash,
    original_title,
    short_context,
    published_at,
    status,
    attempt_count,
    processing_prompt_version,
    created_at,
    updated_at
)
SELECT
    source.company_id,
    log.source_id,
    log.article_url,
    log.article_url_hash,
    log.original_title,
    log.short_context,
    log.published_at,
    'AI_FAILED',
    0,
    'v1',
    log.created_at,
    log.created_at
FROM article_collections log
JOIN blog_sources source ON source.id = log.source_id
WHERE log.decision_status = 'AI_FAILED'
  AND log.id = (
      SELECT MIN(previous.id)
      FROM article_collections previous
      JOIN blog_sources previous_source ON previous_source.id = previous.source_id
      WHERE previous.decision_status = 'AI_FAILED'
        AND previous_source.company_id = source.company_id
        AND previous.article_url_hash = log.article_url_hash
  )
  AND NOT EXISTS (
      SELECT 1
      FROM article_ai_decisions decision
      WHERE decision.company_id = source.company_id
        AND decision.article_url_hash = log.article_url_hash
        AND decision.prompt_version = 'v1'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM articles article
      WHERE article.company_id = source.company_id
        AND article.article_url_hash = log.article_url_hash
  );
