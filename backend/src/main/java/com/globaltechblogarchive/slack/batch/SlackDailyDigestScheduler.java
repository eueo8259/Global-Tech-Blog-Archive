package com.globaltechblogarchive.slack.batch;

import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "slack.daily-digest.enabled", havingValue = "true")
public class SlackDailyDigestScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyDigestJob;
    private final Job retryJob;
    private final SlackDailyDigestProperties properties;
    private final Clock clock;

    public SlackDailyDigestScheduler(
            JobLauncher jobLauncher,
            @Qualifier("slackDailyDigestJob") Job dailyDigestJob,
            @Qualifier("slackDeliveryRetryJob") Job retryJob,
            SlackDailyDigestProperties properties,
            @Qualifier("slackDailyDigestClock") Clock clock
    ) {
        this.jobLauncher = jobLauncher;
        this.dailyDigestJob = dailyDigestJob;
        this.retryJob = retryJob;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${slack.daily-digest.cron:0 0 9 * * *}",
            zone = "${slack.daily-digest.zone:Asia/Seoul}"
    )
    public void launchDailyDigest() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate deliveryDate = now.toLocalDate();
        LocalDateTime windowEndedAt = deliveryDate.atTime(properties.cutoffTime());
        JobParameters parameters = new JobParametersBuilder()
                .addString("deliveryDate", deliveryDate.toString(), true)
                .addString("windowEndedAt", windowEndedAt.toString(), false)
                .addString("executionNow", windowEndedAt.toString(), false)
                .toJobParameters();
        launch(dailyDigestJob, parameters, "Daily Digest");
    }

    @Scheduled(
            cron = "${slack.daily-digest.retry-cron:0 */5 * * * *}",
            zone = "${slack.daily-digest.zone:Asia/Seoul}"
    )
    public void launchRetry() {
        ZonedDateTime now = ZonedDateTime.now(clock)
                .withSecond(0)
                .withNano(0);
        JobParameters parameters = new JobParametersBuilder()
                .addString("retrySlot", now.toString(), true)
                .addString("executionNow", now.toLocalDateTime().toString(), false)
                .toJobParameters();
        launch(retryJob, parameters, "Delivery 재시도");
    }

    private void launch(Job job, JobParameters parameters, String label) {
        try {
            jobLauncher.run(job, parameters);
        } catch (Exception exception) {
            log.error("Slack {} Job 실행 실패", label, exception);
        }
    }
}
