package com.globaltechblogarchive.slack.application.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.domain.SlackDailyDigestRun;
import com.globaltechblogarchive.slack.domain.SlackDailyDigestRunStatus;
import com.globaltechblogarchive.slack.repository.SlackDailyDigestRunRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SlackDailyDigestRunStateServiceTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 8, 3);
    private static final LocalDateTime STARTED_AT = DELIVERY_DATE.atTime(9, 0);

    private final SlackDailyDigestRunRepository repository = mock(SlackDailyDigestRunRepository.class);
    private final SlackDailyDigestRunStateService service = new SlackDailyDigestRunStateService(repository);

    @Test
    void startCreatesFirstRunForDeliveryDate() {
        when(repository.findByDeliveryDate(DELIVERY_DATE)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            SlackDailyDigestRun run = invocation.getArgument(0);
            ReflectionTestUtils.setField(run, "id", 1L);
            return run;
        });

        Optional<Long> runId = service.start(DELIVERY_DATE, STARTED_AT, STARTED_AT);

        assertThat(runId).contains(1L);
    }

    @Test
    void startSkipsCompletedRun() {
        SlackDailyDigestRun run = run();
        run.complete(STARTED_AT.plusMinutes(1), 0, 1, 1);
        when(repository.findByDeliveryDate(DELIVERY_DATE)).thenReturn(Optional.of(run));

        Optional<Long> runId = service.start(DELIVERY_DATE, STARTED_AT, STARTED_AT.plusHours(3));

        assertThat(runId).isEmpty();
        assertThat(run.getStatus()).isEqualTo(SlackDailyDigestRunStatus.COMPLETED);
    }

    @Test
    void startRestartsRunningRunImmediately() {
        SlackDailyDigestRun run = run();
        when(repository.findByDeliveryDate(DELIVERY_DATE)).thenReturn(Optional.of(run));

        Optional<Long> runId = service.start(DELIVERY_DATE, STARTED_AT, STARTED_AT.plusMinutes(1));

        assertThat(runId).contains(1L);
        assertThat(run.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void startRestartsFailedRun() {
        SlackDailyDigestRun run = run();
        run.fail(STARTED_AT.plusMinutes(1), "ERROR", "failed");
        when(repository.findByDeliveryDate(DELIVERY_DATE)).thenReturn(Optional.of(run));

        Optional<Long> runId = service.start(DELIVERY_DATE, STARTED_AT, STARTED_AT.plusMinutes(5));

        assertThat(runId).contains(1L);
        assertThat(run.getStatus()).isEqualTo(SlackDailyDigestRunStatus.RUNNING);
        assertThat(run.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void completeUpdatesStoredRun() {
        SlackDailyDigestRun run = run();
        when(repository.findById(1L)).thenReturn(Optional.of(run));

        service.complete(1L, STARTED_AT.plusMinutes(1), 1, 2, 3);

        assertThat(run.getStatus()).isEqualTo(SlackDailyDigestRunStatus.COMPLETED);
        verify(repository).findById(1L);
    }

    private SlackDailyDigestRun run() {
        SlackDailyDigestRun run = SlackDailyDigestRun.start(DELIVERY_DATE, STARTED_AT, STARTED_AT);
        ReflectionTestUtils.setField(run, "id", 1L);
        return run;
    }

}
