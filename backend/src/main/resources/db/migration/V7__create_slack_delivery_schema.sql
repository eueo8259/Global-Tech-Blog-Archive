CREATE TABLE slack_deliveries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slack_channel_id BIGINT NOT NULL,
    delivery_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    window_started_at DATETIME(6) NOT NULL,
    window_ended_at DATETIME(6) NOT NULL,
    processing_started_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    next_retry_at DATETIME(6) NULL,
    last_error_code VARCHAR(100) NULL,
    last_error_message VARCHAR(500) NULL,
    slack_message_ts VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_slack_deliveries_channel_date
        UNIQUE (slack_channel_id, delivery_date),
    CONSTRAINT fk_slack_deliveries_channel
        FOREIGN KEY (slack_channel_id) REFERENCES slack_channels (id),
    INDEX idx_slack_deliveries_status_retry (status, next_retry_at, id),
    INDEX idx_slack_deliveries_channel_sent_window
        (slack_channel_id, status, window_ended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
