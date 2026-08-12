ALTER TABLE slack_deliveries
    ADD COLUMN preparation_failure_count INT NOT NULL DEFAULT 0 AFTER attempt_count;
