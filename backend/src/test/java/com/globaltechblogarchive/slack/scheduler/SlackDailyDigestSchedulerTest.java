package com.globaltechblogarchive.slack.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.application.digest.SlackDailyDigestRunResult;
import com.globaltechblogarchive.slack.application.digest.SlackDailyDigestService;
import com.globaltechblogarchive.slack.application.digest.SlackDeliveryRetryResult;
import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SlackDailyDigestSchedulerTest {

    private final SlackDailyDigestService service = mock(SlackDailyDigestService.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-12T15:30:45Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final SlackDailyDigestScheduler scheduler = new SlackDailyDigestScheduler(
            service,
            properties(),
            clock
    );

    @Test
    void runDailyDigestUsesConfiguredDateAndCutoffTime() {
        LocalDate deliveryDate = LocalDate.of(2026, 7, 13);
        LocalDateTime windowEndedAt = deliveryDate.atTime(9, 0);
        LocalDateTime executionNow = deliveryDate.atTime(0, 30, 45);
        when(service.runDaily(deliveryDate, windowEndedAt, executionNow))
                .thenReturn(new SlackDailyDigestRunResult(true, 0, 0, 0));

        scheduler.runDailyDigest();

        verify(service).runDaily(deliveryDate, windowEndedAt, executionNow);
    }

    @Test
    void runRetryTruncatesSecondsAndNanos() {
        when(service.runRetry(LocalDateTime.of(2026, 7, 13, 0, 30)))
                .thenReturn(new SlackDeliveryRetryResult(0, 0));

        scheduler.runRetry();

        verify(service).runRetry(LocalDateTime.of(2026, 7, 13, 0, 30));
    }

    @Test
    void runRetryCatchesUpDailyDigestAfterCutoff() {
        SlackDailyDigestScheduler afterCutoffScheduler = schedulerAt("2026-07-13T01:05:00Z");
        LocalDate deliveryDate = LocalDate.of(2026, 7, 13);
        LocalDateTime windowEndedAt = deliveryDate.atTime(9, 0);
        LocalDateTime executionNow = deliveryDate.atTime(10, 5);
        when(service.runDaily(deliveryDate, windowEndedAt, executionNow))
                .thenReturn(new SlackDailyDigestRunResult(true, 1, 2, 3));

        afterCutoffScheduler.runRetry();

        verify(service).runDaily(deliveryDate, windowEndedAt, executionNow);
        verify(service, never()).runRetry(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runRetryProcessesDeliveriesWhenDailyDigestIsAlreadyCompleted() {
        SlackDailyDigestScheduler afterCutoffScheduler = schedulerAt("2026-07-13T01:05:00Z");
        LocalDate deliveryDate = LocalDate.of(2026, 7, 13);
        LocalDateTime windowEndedAt = deliveryDate.atTime(9, 0);
        LocalDateTime executionNow = deliveryDate.atTime(10, 5);
        when(service.runDaily(deliveryDate, windowEndedAt, executionNow))
                .thenReturn(SlackDailyDigestRunResult.skipped());
        when(service.runRetry(executionNow)).thenReturn(new SlackDeliveryRetryResult(1, 2));

        afterCutoffScheduler.runRetry();

        verify(service).runDaily(deliveryDate, windowEndedAt, executionNow);
        verify(service).runRetry(executionNow);
    }

    @Test
    void failedDailyDigestRunsAgainOnNextRetrySchedule() {
        SlackDailyDigestScheduler afterCutoffScheduler = schedulerAt("2026-07-13T00:00:00Z");
        LocalDate deliveryDate = LocalDate.of(2026, 7, 13);
        LocalDateTime executionNow = deliveryDate.atTime(9, 0);
        when(service.runDaily(deliveryDate, executionNow, executionNow))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(new SlackDailyDigestRunResult(true, 0, 1, 1));

        afterCutoffScheduler.runDailyDigest();
        afterCutoffScheduler.runRetry();

        verify(service, times(2)).runDaily(deliveryDate, executionNow, executionNow);
        verify(service, never()).runRetry(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retryDoesNotStartWhileDailyRunIsExecuting() {
        LocalDate deliveryDate = LocalDate.of(2026, 7, 13);
        LocalDateTime windowEndedAt = deliveryDate.atTime(9, 0);
        LocalDateTime executionNow = deliveryDate.atTime(0, 30, 45);
        when(service.runDaily(deliveryDate, windowEndedAt, executionNow)).thenAnswer(invocation -> {
            scheduler.runRetry();
            return new SlackDailyDigestRunResult(true, 0, 0, 0);
        });

        scheduler.runDailyDigest();

        verify(service, never()).runRetry(org.mockito.ArgumentMatchers.any());
    }

    private SlackDailyDigestProperties properties() {
        return new SlackDailyDigestProperties(
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
        );
    }

    private SlackDailyDigestScheduler schedulerAt(String instant) {
        Clock fixedClock = Clock.fixed(
                Instant.parse(instant),
                ZoneId.of("Asia/Seoul")
        );
        return new SlackDailyDigestScheduler(service, properties(), fixedClock);
    }
}
