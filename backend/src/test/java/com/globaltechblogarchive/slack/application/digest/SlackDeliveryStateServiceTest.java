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
                    Duration.ofMinutes(15)
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
        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.PROCESSING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
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
        delivery.markRetryWaiting(now, "HTTP_503", "server error");
        delivery.startProcessing(now);
        delivery.markRetryWaiting(now, "HTTP_503", "server error");
        delivery.startProcessing(now);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        service.markFailure(1L, now, true, null, "HTTP_503", "server error");

        assertThat(delivery.getStatus()).isEqualTo(SlackDeliveryStatus.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(3);
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
