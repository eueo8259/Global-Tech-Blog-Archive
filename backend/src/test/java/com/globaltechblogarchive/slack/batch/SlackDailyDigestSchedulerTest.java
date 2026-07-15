package com.globaltechblogarchive.slack.batch;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

class SlackDailyDigestSchedulerTest {

    @Test
    void launchDailyDigestUsesAsiaSeoulDateAndCutoffTime() throws Exception {
        JobLauncher jobLauncher = mock(JobLauncher.class);
        Job dailyJob = mock(Job.class);
        Job retryJob = mock(Job.class);
        SlackDailyDigestProperties properties = new SlackDailyDigestProperties(
                "Asia/Seoul",
                LocalTime.of(9, 0),
                3,
                Duration.ofMinutes(5),
                Duration.ofMinutes(15)
        );
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-12T15:30:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        SlackDailyDigestScheduler scheduler = new SlackDailyDigestScheduler(
                jobLauncher,
                dailyJob,
                retryJob,
                properties,
                clock
        );

        scheduler.launchDailyDigest();

        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(same(dailyJob), parametersCaptor.capture());
        JobParameters parameters = parametersCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(parameters.getString("deliveryDate"))
                .isEqualTo("2026-07-13");
        org.assertj.core.api.Assertions.assertThat(parameters.getString("windowEndedAt"))
                .isEqualTo("2026-07-13T09:00");
    }
}
