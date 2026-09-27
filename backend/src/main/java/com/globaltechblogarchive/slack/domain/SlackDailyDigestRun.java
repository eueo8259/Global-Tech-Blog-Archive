package com.globaltechblogarchive.slack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "slack_daily_digest_runs",
        indexes = @Index(name = "idx_slack_daily_digest_runs_status_started", columnList = "status,started_at"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_slack_daily_digest_runs_delivery_date",
                columnNames = "delivery_date"
        )
)
public class SlackDailyDigestRun {

    private static final int ERROR_CODE_MAX_LENGTH = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "window_ended_at", nullable = false)
    private LocalDateTime windowEndedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SlackDailyDigestRunStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "recovered_delivery_count", nullable = false)
    private int recoveredDeliveryCount;

    @Column(name = "created_delivery_count", nullable = false)
    private int createdDeliveryCount;

    @Column(name = "ready_delivery_count", nullable = false)
    private int readyDeliveryCount;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "last_error_code", length = ERROR_CODE_MAX_LENGTH)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SlackDailyDigestRun start(
            LocalDate deliveryDate,
            LocalDateTime windowEndedAt,
            LocalDateTime startedAt
    ) {
        SlackDailyDigestRun run = new SlackDailyDigestRun();
        run.deliveryDate = deliveryDate;
        run.windowEndedAt = windowEndedAt;
        run.status = SlackDailyDigestRunStatus.RUNNING;
        run.attemptCount = 1;
        run.startedAt = startedAt;
        run.createdAt = startedAt;
        run.updatedAt = startedAt;
        return run;
    }

    public boolean canRestart() {
        return status == SlackDailyDigestRunStatus.FAILED
                || status == SlackDailyDigestRunStatus.RUNNING;
    }

    public void restart(LocalDateTime windowEndedAt, LocalDateTime restartedAt) {
        if (status == SlackDailyDigestRunStatus.COMPLETED) {
            throw new IllegalStateException("완료된 Daily Digest 실행은 다시 시작할 수 없습니다.");
        }
        this.windowEndedAt = windowEndedAt;
        status = SlackDailyDigestRunStatus.RUNNING;
        attemptCount++;
        recoveredDeliveryCount = 0;
        createdDeliveryCount = 0;
        readyDeliveryCount = 0;
        startedAt = restartedAt;
        endedAt = null;
        lastErrorCode = null;
        lastErrorMessage = null;
        updatedAt = restartedAt;
    }

    public void complete(
            LocalDateTime completedAt,
            int recoveredCount,
            int createdCount,
            int readyCount
    ) {
        requireRunning();
        status = SlackDailyDigestRunStatus.COMPLETED;
        recoveredDeliveryCount = recoveredCount;
        createdDeliveryCount = createdCount;
        readyDeliveryCount = readyCount;
        endedAt = completedAt;
        updatedAt = completedAt;
    }

    public void fail(LocalDateTime failedAt, String errorCode, String errorMessage) {
        requireRunning();
        status = SlackDailyDigestRunStatus.FAILED;
        endedAt = failedAt;
        lastErrorCode = truncate(errorCode, ERROR_CODE_MAX_LENGTH);
        lastErrorMessage = truncate(errorMessage, ERROR_MESSAGE_MAX_LENGTH);
        updatedAt = failedAt;
    }

    private void requireRunning() {
        if (status != SlackDailyDigestRunStatus.RUNNING) {
            throw new IllegalStateException("RUNNING 상태의 Daily Digest 실행만 완료할 수 있습니다.");
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
