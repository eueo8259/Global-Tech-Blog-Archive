package com.globaltechblogarchive.slack.application.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SlackDeliveryStateServiceTest {

    private final SlackDeliveryRepository repository = mock(SlackDeliveryRepository.class);
    private final SlackDeliveryStateService service = new SlackDeliveryStateService(
            repository,
            new SlackDailyDigestProperties(
                    "Asia/Seoul",
                    LocalTime.of(9, 0),
                    3,
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(5),
                    Duration.ofHours(1),
                    3,
                    Duration.ofMinutes(1),
                    15
            )
    );

    @Test
    void claimTransitionsPendingDeliveryToProcessing() {
        SlackDelivery delivery = delivery();
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);

        Optional<ClaimedSlackDelivery> claimed = service.claim(1L, now);

        assertThat(claimed).isPresent();
        assertThat(claimed.orElseThrow().slackChannelId()).isEqualTo("C123");
        assertThat(claimed.orElseThrow().deliveryKey()).isEqualTo(delivery.getDeliveryKey());
        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.PROCESSING);
        assertThat(delivery.getAttemptCount()).isZero();
    }

    @Test
    void startSendAttemptIncreasesCountImmediatelyBeforeSlackCall() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        delivery.startProcessing(now.minusSeconds(1));
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        boolean started = service.startSendAttempt(1L, now);

        assertThat(started).isTrue();
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getProcessingStartedAt()).isEqualTo(now);
    }

    @Test
    void markFailureSchedulesRetryBeforeMaximumAttempts() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        delivery.startProcessing(now);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        service.markFailure(1L, now, true, null, "HTTP_503", "server error");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.RETRY_WAITING);
        assertThat(delivery.getNextRetryAt()).isEqualTo(now.plusMinutes(5));
    }

    @Test
    void markFailureStopsRetryAtMaximumAttempts() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        delivery.startProcessing(now);
        delivery.startSendAttempt(now, 3);
        delivery.markRetryWaiting(now, "HTTP_503", "server error");
        delivery.startProcessing(now);
        delivery.startSendAttempt(now, 3);
        delivery.markRetryWaiting(now, "HTTP_503", "server error");
        delivery.startProcessing(now);
        delivery.startSendAttempt(now, 3);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        service.markFailure(1L, now, true, null, "HTTP_503", "server error");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(3);
    }

    @Test
    void preparationFailureStopsRetryAtMaximumAttemptsWithoutSlackAttempts() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 0);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        for (int attempt = 0; attempt < 3; attempt++) {
            delivery.startProcessing(now.plusMinutes(attempt * 5L));
            service.recordPreparationFailure(
                    1L,
                    now.plusMinutes(attempt * 5L),
                    "UNEXPECTED_ERROR",
                    "database unavailable"
            );
        }

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getPreparationFailureCount()).isEqualTo(3);
        assertThat(delivery.getAttemptCount()).isZero();
        assertThat(delivery.getNextRetryAt()).isNull();
        assertThat(delivery.getLastErrorCode()).isEqualTo("UNEXPECTED_ERROR");
    }

    @Test
    void recoverStaleProcessingSchedulesVerification() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 20);
        delivery.startProcessing(now.minusMinutes(20));
        when(repository.findByStatusAndProcessingStartedAtBefore(
                SlackDeliveryStatus.PROCESSING,
                now.minusMinutes(15)
        )).thenReturn(List.of(delivery));

        int recoveredCount = service.recoverStaleProcessing(now);

        assertThat(recoveredCount).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.VERIFYING);
        assertThat(delivery.getProcessingStartedAt()).isEqualTo(now.minusMinutes(20));
        assertThat(delivery.getNextVerificationAt()).isEqualTo(now);
        assertThat(delivery.getLastErrorCode()).isEqualTo("STALE_PROCESSING");
    }

    @Test
    void recoverStaleProcessingVerifiesEvenAtMaximumAttempts() {
        SlackDelivery delivery = delivery();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 9, 20);
        startThirdAttempt(delivery, now.minusMinutes(20));
        when(repository.findByStatusAndProcessingStartedAtBefore(
                SlackDeliveryStatus.PROCESSING,
                now.minusMinutes(15)
        )).thenReturn(List.of(delivery));

        int recoveredCount = service.recoverStaleProcessing(now);

        assertThat(recoveredCount).isEqualTo(1);
        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.VERIFYING);
        assertThat(delivery.getAttemptCount()).isEqualTo(3);
        assertThat(delivery.getLastErrorCode()).isEqualTo("STALE_PROCESSING");
    }

    private void startThirdAttempt(SlackDelivery delivery, LocalDateTime startedAt) {
        delivery.startProcessing(startedAt.minusMinutes(10));
        delivery.startSendAttempt(startedAt.minusMinutes(10), 3);
        delivery.markRetryWaiting(startedAt.minusMinutes(9), "HTTP_503", "server error");
        delivery.startProcessing(startedAt.minusMinutes(5));
        delivery.startSendAttempt(startedAt.minusMinutes(5), 3);
        delivery.markRetryWaiting(startedAt.minusMinutes(4), "HTTP_503", "server error");
        delivery.startProcessing(startedAt);
        delivery.startSendAttempt(startedAt, 3);
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
        ReflectionTestUtils.setField(channel, "id", 10L);
        SlackDelivery delivery = SlackDelivery.pending(
                channel,
                LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 13, 9, 0),
                LocalDateTime.of(2026, 7, 14, 9, 0)
        );
        ReflectionTestUtils.setField(delivery, "id", 1L);
        return delivery;
    }
}
