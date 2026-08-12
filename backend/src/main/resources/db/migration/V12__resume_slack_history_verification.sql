ALTER TABLE slack_deliveries
    ADD COLUMN history_cursor VARCHAR(500) NULL AFTER next_verification_at,
    ADD COLUMN history_latest_at DATETIME(6) NULL AFTER history_cursor;

UPDATE slack_deliveries
SET preparation_failure_count = attempt_count,
    attempt_count = 0
WHERE status = 'RETRY_WAITING'
  AND last_error_code = 'UNEXPECTED_ERROR';
