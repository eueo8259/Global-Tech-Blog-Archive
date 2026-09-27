CREATE TABLE slack_daily_digest_runs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    delivery_date DATE NOT NULL,
    window_ended_at DATETIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    recovered_delivery_count INT NOT NULL DEFAULT 0,
    created_delivery_count INT NOT NULL DEFAULT 0,
    ready_delivery_count INT NOT NULL DEFAULT 0,
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    last_error_code VARCHAR(100) NULL,
    last_error_message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_slack_daily_digest_runs_delivery_date UNIQUE (delivery_date),
    INDEX idx_slack_daily_digest_runs_status_started (status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
