package com.globaltechblogarchive.slack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SlackDailyDigestRunTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 8, 3);
    private static final LocalDateTime STARTED_AT = DELIVERY_DATE.atTime(9, 0);

    @Test
    void completeStoresExecutionCounts() {
        SlackDailyDigestRun run = SlackDailyDigestRun.start(DELIVERY_DATE, STARTED_AT, STARTED_AT);

        run.complete(STARTED_AT.plusMinutes(1), 2, 3, 4);

        assertThat(run.getStatus()).isEqualTo(SlackDailyDigestRunStatus.COMPLETED);
        assertThat(run.getRecoveredDeliveryCount()).isEqualTo(2);
        assertThat(run.getCreatedDeliveryCount()).isEqualTo(3);
        assertThat(run.getReadyDeliveryCount()).isEqualTo(4);
        assertThat(run.getEndedAt()).isEqualTo(STARTED_AT.plusMinutes(1));
    }

    @Test
    void failedRunCanRestartAndIncrementsAttemptCount() {
        SlackDailyDigestRun run = SlackDailyDigestRun.start(DELIVERY_DATE, STARTED_AT, STARTED_AT);
        run.fail(STARTED_AT.plusMinutes(1), "ERROR", "failure");

        run.restart(STARTED_AT, STARTED_AT.plusMinutes(2));

        assertThat(run.getStatus()).isEqualTo(SlackDailyDigestRunStatus.RUNNING);
        assertThat(run.getAttemptCount()).isEqualTo(2);
        assertThat(run.getEndedAt()).isNull();
        assertThat(run.getLastErrorCode()).isNull();
    }

    @Test
    void runningRunCanRestartUntilItCompletes() {
        SlackDailyDigestRun run = SlackDailyDigestRun.start(DELIVERY_DATE, STARTED_AT, STARTED_AT);

        assertThat(run.canRestart()).isTrue();

        run.complete(STARTED_AT.plusMinutes(1), 0, 0, 0);

        assertThat(run.canRestart()).isFalse();
    }
}
