package com.globaltechblogarchive.slack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SlackDeliveryTest {

    @Test
    void pendingCreatesStableUuidDeliveryKey() {
        SlackDelivery delivery = delivery();
        String deliveryKey = delivery.getDeliveryKey();

        assertThat(UUID.fromString(deliveryKey).toString()).isEqualTo(deliveryKey);

        startAttempt(delivery, LocalDateTime.of(2026, 7, 14, 9, 0));
        delivery.markRetryWaiting(
                LocalDateTime.of(2026, 7, 14, 9, 5),
                "HTTP_429",
                "retry"
        );
        startAttempt(delivery, LocalDateTime.of(2026, 7, 14, 9, 5));

        assertThat(delivery.getDeliveryKey()).isEqualTo(deliveryKey);
    }

    @Test
    void pendingDeliveryTransitionsThroughRetryToSent() {
        SlackDelivery delivery = delivery();
        LocalDateTime firstAttempt = LocalDateTime.of(2026, 7, 14, 9, 0);

        startAttempt(delivery, firstAttempt);
        delivery.markRetryWaiting(
                firstAttempt.plusMinutes(5),
                "HTTP_429",
                "호출 한도 초과"
        );

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.canStart(firstAttempt.plusMinutes(4))).isFalse();
        assertThat(delivery.canStart(firstAttempt.plusMinutes(5))).isTrue();

        startAttempt(delivery, firstAttempt.plusMinutes(5));
        delivery.markSent(firstAttempt.plusMinutes(6), "1720937160.000100");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.SENT);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(delivery.getSentAt()).isEqualTo(firstAttempt.plusMinutes(6));
        assertThat(delivery.getSlackMessageTs()).isEqualTo("1720937160.000100");
        assertThat(delivery.getLastErrorCode()).isNull();
    }

    @Test
    void staleProcessingTransitionsToVerifyingAndKeepsAttemptedAt() {
        SlackDelivery delivery = delivery();
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 14, 9, 0);
        startAttempt(delivery, startedAt);

        delivery.recoverStaleProcessing(startedAt.plusMinutes(20));

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.VERIFYING);
        assertThat(delivery.getProcessingStartedAt()).isEqualTo(startedAt);
        assertThat(delivery.getNextVerificationAt()).isEqualTo(startedAt.plusMinutes(20));
        assertThat(delivery.getLastErrorCode()).isEqualTo("STALE_PROCESSING");
    }

    @Test
    void successfulVerificationMissesRetryOnlyAfterThirdCheck() {
        SlackDelivery delivery = delivery();
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 14, 9, 0);
        startAttempt(delivery, startedAt);
        delivery.markVerifying(startedAt, null, "NETWORK_ERROR", "timeout");

        delivery.recordVerificationNotFound(startedAt.plusMinutes(5), 3, 3);
        delivery.recordVerificationNotFound(startedAt.plusMinutes(10), 3, 3);

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.VERIFYING);
        assertThat(delivery.getVerificationCount()).isEqualTo(2);

        delivery.recordVerificationNotFound(startedAt.plusMinutes(15), 3, 3);

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getVerificationCount()).isEqualTo(3);
        assertThat(delivery.getNextRetryAt()).isEqualTo(startedAt.plusMinutes(15));
    }

    @Test
    void lookupFailureSchedulesNextVerificationWithoutIncreasingCount() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        startAttempt(delivery, now);
        delivery.markVerifying(now, null, "NETWORK_ERROR", "timeout");

        delivery.scheduleNextVerification(now.plusHours(1), "missing_scope", "scope missing");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.VERIFYING);
        assertThat(delivery.getVerificationCount()).isZero();
        assertThat(delivery.getNextVerificationAt()).isEqualTo(now.plusHours(1));
        assertThat(delivery.getLastErrorCode()).isEqualTo("missing_scope");
    }

    @Test
    void historyPaginationResumesWithFixedUpperBoundAndClearsAfterCompleteMiss() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        startAttempt(delivery, now);
        delivery.markVerifying(now, null, "NETWORK_ERROR", "timeout");

        delivery.continueVerification(
                now.plusMinutes(5),
                "cursor-2",
                now.plusMinutes(1)
        );

        assertThat(delivery.getHistoryCursor()).isEqualTo("cursor-2");
        assertThat(delivery.getHistoryLatestAt()).isEqualTo(now.plusMinutes(1));
        assertThat(delivery.getVerificationCount()).isZero();

        delivery.recordVerificationNotFound(now.plusMinutes(10), 3, 3);

        assertThat(delivery.getHistoryCursor()).isNull();
        assertThat(delivery.getHistoryLatestAt()).isNull();
        assertThat(delivery.getVerificationCount()).isEqualTo(1);
    }

    @Test
    void foundMessageTransitionsVerifyingDeliveryToSent() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        startAttempt(delivery, now);
        delivery.markVerifying(now, null, "NETWORK_ERROR", "timeout");

        delivery.markSent(now.plusMinutes(1), "1720937160.000100");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.SENT);
        assertThat(delivery.getSlackMessageTs()).isEqualTo("1720937160.000100");
        assertThat(delivery.getNextVerificationAt()).isNull();
    }

    @Test
    void processingDeliveryCanTransitionToFailed() {
        SlackDelivery delivery = delivery();
        startAttempt(delivery, LocalDateTime.of(2026, 7, 14, 9, 0));

        delivery.markFailed("invalid_auth", "인증 실패");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getNextRetryAt()).isNull();
    }

    @Test
    void claimDoesNotIncreaseAttemptUntilActualSendStarts() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);

        delivery.startProcessing(now);

        assertThat(delivery.getAttemptCount()).isZero();

        assertThat(delivery.startSendAttempt(now.plusSeconds(1), 3)).isTrue();
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getProcessingStartedAt()).isEqualTo(now.plusSeconds(1));
    }

    @Test
    void thirdVerificationMissFailsWhenSendAttemptsAreExhausted() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        reachThirdVerifyingAttempt(delivery, now);

        delivery.recordVerificationNotFound(now.plusMinutes(5), 3, 3);
        delivery.recordVerificationNotFound(now.plusMinutes(10), 3, 3);
        delivery.recordVerificationNotFound(now.plusMinutes(15), 3, 3);

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(3);
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.getLastErrorCode())
                .isEqualTo("SLACK_MESSAGE_NOT_FOUND_MAX_ATTEMPTS");
    }

    @Test
    void sendAttemptDoesNotExceedMaximum() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        reachThirdVerifyingAttempt(delivery, now);
        delivery.recordVerificationNotFound(now.plusMinutes(5), 1, 4);
        delivery.startProcessing(now.plusMinutes(5));

        assertThat(delivery.startSendAttempt(now.plusMinutes(5), 3)).isFalse();
        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(3);
        assertThat(delivery.getLastErrorCode()).isEqualTo("SLACK_SEND_MAX_ATTEMPTS");
    }

    @Test
    void preparationFailuresAreLimitedSeparatelyFromSlackSendAttempts() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);

        delivery.startProcessing(now);
        delivery.recordPreparationFailure(now.plusMinutes(5), 3, "UNEXPECTED_ERROR", "query failed");
        delivery.startProcessing(now.plusMinutes(5));
        delivery.recordPreparationFailure(now.plusMinutes(10), 3, "UNEXPECTED_ERROR", "query failed");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getPreparationFailureCount()).isEqualTo(2);
        assertThat(delivery.getAttemptCount()).isZero();

        delivery.startProcessing(now.plusMinutes(10));
        delivery.recordPreparationFailure(now.plusMinutes(15), 3, "UNEXPECTED_ERROR", "query failed");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getPreparationFailureCount()).isEqualTo(3);
        assertThat(delivery.getAttemptCount()).isZero();
        assertThat(delivery.getNextRetryAt()).isNull();
    }

    private void reachThirdVerifyingAttempt(SlackDelivery delivery, LocalDateTime now) {
        startAttempt(delivery, now.minusMinutes(20));
        delivery.markRetryWaiting(now.minusMinutes(19), "HTTP_429", "retry");
        startAttempt(delivery, now.minusMinutes(10));
        delivery.markRetryWaiting(now.minusMinutes(9), "HTTP_429", "retry");
        startAttempt(delivery, now);
        delivery.markVerifying(now, null, "NETWORK_ERROR", "timeout");
    }

    private void startAttempt(SlackDelivery delivery, LocalDateTime startedAt) {
        delivery.startProcessing(startedAt);
        assertThat(delivery.startSendAttempt(startedAt, 3)).isTrue();
    }

    private SlackDelivery delivery() {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T123",
                "TechPort",
                "encrypted-token",
                "B123",
                "commands,chat:write",
                LocalDateTime.of(2026, 7, 13, 10, 0)
        );
        SlackChannel channel = SlackChannel.create(workspace, "C123", "tech-news");
        return SlackDelivery.pending(
                channel,
                LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 13, 9, 0),
                LocalDateTime.of(2026, 7, 14, 9, 0)
        );
    }
}
