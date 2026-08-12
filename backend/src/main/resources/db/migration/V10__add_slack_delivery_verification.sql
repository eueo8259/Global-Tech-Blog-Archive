ALTER TABLE slack_deliveries
    ADD COLUMN delivery_key VARCHAR(36) NULL AFTER attempt_count,
    ADD COLUMN verification_count INT NOT NULL DEFAULT 0 AFTER delivery_key,
    ADD COLUMN next_verification_at DATETIME(6) NULL AFTER next_retry_at;

UPDATE slack_deliveries
SET delivery_key = UUID()
WHERE delivery_key IS NULL;

UPDATE slack_deliveries
SET status = 'VERIFYING',
    processing_started_at = COALESCE(processing_started_at, sent_at, updated_at),
    next_verification_at = updated_at
WHERE status = 'SENT_UNCONFIRMED';

ALTER TABLE slack_deliveries
    MODIFY COLUMN delivery_key VARCHAR(36) NOT NULL,
    ADD CONSTRAINT uq_slack_deliveries_delivery_key UNIQUE (delivery_key),
    ADD INDEX idx_slack_deliveries_status_verification
        (status, next_verification_at, id);
