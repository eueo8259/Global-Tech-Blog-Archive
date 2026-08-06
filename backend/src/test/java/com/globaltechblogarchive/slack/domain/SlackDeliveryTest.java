package com.globaltechblogarchive.slack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SlackDeliveryTest {

    @Test
    void pendingDeliveryTransitionsThroughRetryToSent() {
        SlackDelivery delivery = delivery();
        LocalDateTime firstAttempt = LocalDateTime.of(2026, 7, 14, 9, 0);

        delivery.startProcessing(firstAttempt);
        delivery.markRetryWaiting(
                firstAttempt.plusMinutes(5),
                "HTTP_429",
                "호출 한도 초과"
        );

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.canStart(firstAttempt.plusMinutes(4))).isFalse();
        assertThat(delivery.canStart(firstAttempt.plusMinutes(5))).isTrue();

        delivery.startProcessing(firstAttempt.plusMinutes(5));
        delivery.markSent(firstAttempt.plusMinutes(6), "1720937160.000100");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.SENT);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(delivery.getSentAt()).isEqualTo(firstAttempt.plusMinutes(6));
        assertThat(delivery.getSlackMessageTs()).isEqualTo("1720937160.000100");
        assertThat(delivery.getLastErrorCode()).isNull();
    }

    @Test
    void staleProcessingTransitionsToRetryWaiting() {
        SlackDelivery delivery = delivery();
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 14, 9, 0);
        delivery.startProcessing(startedAt);

        delivery.recoverStaleProcessing(startedAt.plusMinutes(20), 3);

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getNextRetryAt()).isEqualTo(startedAt.plusMinutes(20));
        assertThat(delivery.getLastErrorCode()).isEqualTo("STALE_PROCESSING");
    }

    @Test
    void staleProcessingTransitionsToFailedAtMaximumAttempts() {
        SlackDelivery delivery = delivery();
        LocalDateTime startedAt = LocalDateTime.of(2026, 7, 14, 9, 0);
        delivery.startProcessing(startedAt.minusMinutes(10));
        delivery.markRetryWaiting(startedAt.minusMinutes(9), "HTTP_503", "server error");
        delivery.startProcessing(startedAt.minusMinutes(5));
        delivery.markRetryWaiting(startedAt.minusMinutes(4), "HTTP_503", "server error");
        delivery.startProcessing(startedAt);

        delivery.recoverStaleProcessing(startedAt.plusMinutes(20), 3);

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.getLastErrorCode()).isEqualTo("STALE_PROCESSING_MAX_ATTEMPTS");
    }

    @Test
    void sentStatusSaveFailureTransitionsToUnconfirmedWithoutRetry() {
        SlackDelivery delivery = delivery();
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 14, 9, 1);
        delivery.startProcessing(completedAt.minusMinutes(1));

        delivery.markSentUnconfirmed(
                completedAt,
                "1720937160.000100",
                "database unavailable"
        );

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.SENT_UNCONFIRMED);
        assertThat(delivery.getSentAt()).isEqualTo(completedAt);
        assertThat(delivery.getSlackMessageTs()).isEqualTo("1720937160.000100");
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.canStart(completedAt.plusMinutes(5))).isFalse();
        assertThat(delivery.getLastErrorCode()).isEqualTo("SENT_STATUS_SAVE_FAILED");
    }

    @Test
    void processingDeliveryCanTransitionToFailed() {
        SlackDelivery delivery = delivery();
        delivery.startProcessing(LocalDateTime.of(2026, 7, 14, 9, 0));

        delivery.markFailed("invalid_auth", "인증 실패");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getNextRetryAt()).isNull();
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
