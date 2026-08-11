package com.globaltechblogarchive.slack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "slack_deliveries",
        indexes = {
                @Index(
                        name = "idx_slack_deliveries_status_retry",
                        columnList = "status,next_retry_at,id"
                ),
                @Index(
                        name = "idx_slack_deliveries_status_verification",
                        columnList = "status,next_verification_at,id"
                ),
                @Index(
                        name = "idx_slack_deliveries_channel_sent_window",
                        columnList = "slack_channel_id,status,window_ended_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_slack_deliveries_channel_date",
                        columnNames = {"slack_channel_id", "delivery_date"}
                ),
                @UniqueConstraint(
                        name = "uq_slack_deliveries_delivery_key",
                        columnNames = "delivery_key"
                )
        }
)
public class SlackDelivery {

    private static final int ERROR_CODE_MAX_LENGTH = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slack_channel_id", nullable = false)
    private SlackChannel slackChannel;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SlackDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "preparation_failure_count", nullable = false)
    private int preparationFailureCount;

    @Column(name = "delivery_key", nullable = false, length = 36)
    private String deliveryKey;

    @Column(name = "verification_count", nullable = false)
    private int verificationCount;

    @Column(name = "window_started_at", nullable = false)
    private LocalDateTime windowStartedAt;

    @Column(name = "window_ended_at", nullable = false)
    private LocalDateTime windowEndedAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "next_verification_at")
    private LocalDateTime nextVerificationAt;

    @Column(name = "last_error_code", length = ERROR_CODE_MAX_LENGTH)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String lastErrorMessage;

    @Column(name = "slack_message_ts", length = 50)
    private String slackMessageTs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SlackDelivery pending(
            SlackChannel slackChannel,
            LocalDate deliveryDate,
            LocalDateTime windowStartedAt,
            LocalDateTime windowEndedAt
    ) {
        if (!windowEndedAt.isAfter(windowStartedAt)) {
            throw new IllegalArgumentException("발송 범위 종료 시각은 시작 시각보다 뒤여야 합니다.");
        }
        SlackDelivery delivery = new SlackDelivery();
        delivery.slackChannel = slackChannel;
        delivery.deliveryDate = deliveryDate;
        delivery.status = SlackDeliveryStatus.PENDING;
        delivery.deliveryKey = UUID.randomUUID().toString();
        delivery.windowStartedAt = windowStartedAt;
        delivery.windowEndedAt = windowEndedAt;
        return delivery;
    }

    public void startProcessing(LocalDateTime startedAt) {
        if (status != SlackDeliveryStatus.PENDING && status != SlackDeliveryStatus.RETRY_WAITING) {
            throw new IllegalStateException("처리 가능한 Slack Delivery 상태가 아닙니다: " + status);
        }
        status = SlackDeliveryStatus.PROCESSING;
        processingStartedAt = startedAt;
        nextRetryAt = null;
    }

    public boolean startSendAttempt(LocalDateTime startedAt, int maxAttempts) {
        requireProcessing();
        if (attemptCount >= maxAttempts) {
            status = SlackDeliveryStatus.FAILED;
            processingStartedAt = null;
            nextRetryAt = null;
            nextVerificationAt = null;
            setError(
                    "SLACK_SEND_MAX_ATTEMPTS",
                    "Slack 발송 최대 시도 횟수에 도달했습니다."
            );
            return false;
        }
        processingStartedAt = startedAt;
        verificationCount = 0;
        nextVerificationAt = null;
        attemptCount++;
        return true;
    }

    public void markSent(LocalDateTime completedAt, String messageTs) {
        requireProcessingOrVerifying();
        status = SlackDeliveryStatus.SENT;
        sentAt = completedAt;
        slackMessageTs = messageTs;
        processingStartedAt = null;
        nextRetryAt = null;
        verificationCount = 0;
        nextVerificationAt = null;
        clearError();
    }

    public void markVerifying(
            LocalDateTime verificationAt,
            String messageTs,
            String errorCode,
            String errorMessage
    ) {
        requireProcessing();
        status = SlackDeliveryStatus.VERIFYING;
        slackMessageTs = messageTs;
        nextRetryAt = null;
        nextVerificationAt = verificationAt;
        setError(errorCode, errorMessage);
    }

    public void scheduleNextVerification(
            LocalDateTime nextAt,
            String errorCode,
            String errorMessage
    ) {
        requireVerifying();
        nextVerificationAt = nextAt;
        setError(errorCode, errorMessage);
    }

    public void recordVerificationNotFound(
            LocalDateTime nextAt,
            int maxVerificationChecks,
            int maxAttempts
    ) {
        requireVerifying();
        verificationCount++;
        if (verificationCount >= maxVerificationChecks) {
            if (attemptCount >= maxAttempts) {
                status = SlackDeliveryStatus.FAILED;
                processingStartedAt = null;
                nextVerificationAt = null;
                nextRetryAt = null;
                setError(
                        "SLACK_MESSAGE_NOT_FOUND_MAX_ATTEMPTS",
                        "Slack 메시지를 찾지 못했고 최대 발송 시도 횟수에 도달했습니다."
                );
                return;
            }
            status = SlackDeliveryStatus.RETRY_WAITING;
            processingStartedAt = null;
            nextVerificationAt = null;
            nextRetryAt = nextAt;
            setError("SLACK_MESSAGE_NOT_FOUND", "Slack History에서 발송 메시지를 찾지 못했습니다.");
            return;
        }
        nextVerificationAt = nextAt;
        setError("SLACK_MESSAGE_NOT_FOUND", "Slack History에서 발송 메시지를 찾지 못했습니다.");
    }

    public void markRetryWaiting(
            LocalDateTime nextRetryAt,
            String errorCode,
            String errorMessage
    ) {
        requireProcessing();
        status = SlackDeliveryStatus.RETRY_WAITING;
        processingStartedAt = null;
        this.nextRetryAt = nextRetryAt;
        setError(errorCode, errorMessage);
    }

    public void recordPreparationFailure(
            LocalDateTime nextRetryAt,
            int maxAttempts,
            String errorCode,
            String errorMessage
    ) {
        requireProcessing();
        preparationFailureCount++;
        if (preparationFailureCount >= maxAttempts) {
            markFailed(errorCode, errorMessage);
            return;
        }
        markRetryWaiting(nextRetryAt, errorCode, errorMessage);
    }

    public void markFailed(String errorCode, String errorMessage) {
        requireProcessing();
        status = SlackDeliveryStatus.FAILED;
        processingStartedAt = null;
        nextRetryAt = null;
        setError(errorCode, errorMessage);
    }

    public void recoverStaleProcessing(LocalDateTime verificationAt) {
        if (status != SlackDeliveryStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 상태만 복구할 수 있습니다.");
        }
        status = SlackDeliveryStatus.VERIFYING;
        nextRetryAt = null;
        nextVerificationAt = verificationAt;
        setError("STALE_PROCESSING", "처리 중 서버 종료로 Slack 발송 결과 검증 상태로 복구되었습니다.");
    }

    public boolean canStart(LocalDateTime now) {
        if (status == SlackDeliveryStatus.PENDING) {
            return true;
        }
        return status == SlackDeliveryStatus.RETRY_WAITING
                && nextRetryAt != null
                && !nextRetryAt.isAfter(now);
    }

    private void requireProcessing() {
        if (status != SlackDeliveryStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 상태에서만 결과를 기록할 수 있습니다.");
        }
    }

    private void requireVerifying() {
        if (status != SlackDeliveryStatus.VERIFYING) {
            throw new IllegalStateException("VERIFYING 상태에서만 검증 결과를 기록할 수 있습니다.");
        }
    }

    private void requireProcessingOrVerifying() {
        if (status != SlackDeliveryStatus.PROCESSING && status != SlackDeliveryStatus.VERIFYING) {
            throw new IllegalStateException("PROCESSING 또는 VERIFYING 상태에서만 성공을 기록할 수 있습니다.");
        }
    }

    private void setError(String errorCode, String errorMessage) {
        lastErrorCode = truncate(errorCode, ERROR_CODE_MAX_LENGTH);
        lastErrorMessage = truncate(errorMessage, ERROR_MESSAGE_MAX_LENGTH);
    }

    private void clearError() {
        lastErrorCode = null;
        lastErrorMessage = null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
