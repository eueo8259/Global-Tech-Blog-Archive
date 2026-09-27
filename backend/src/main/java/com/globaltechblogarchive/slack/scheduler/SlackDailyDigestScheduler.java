package com.globaltechblogarchive.slack.scheduler;

import com.globaltechblogarchive.slack.application.digest.SlackDailyDigestRunResult;
import com.globaltechblogarchive.slack.application.digest.SlackDailyDigestService;
import com.globaltechblogarchive.slack.application.digest.SlackDeliveryRetryResult;
import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "slack.daily-digest.enabled", havingValue = "true")
public class SlackDailyDigestScheduler {

    private final SlackDailyDigestService digestService;
    private final SlackDailyDigestProperties properties;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SlackDailyDigestScheduler(
            SlackDailyDigestService digestService,
            SlackDailyDigestProperties properties,
            @Qualifier("slackDailyDigestClock") Clock clock
    ) {
        this.digestService = digestService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${slack.daily-digest.cron:0 0 9 * * *}",
            zone = "${slack.daily-digest.zone:Asia/Seoul}"
    )
    public void runDailyDigest() {
        execute("Daily Digest", () -> {
            ZonedDateTime now = ZonedDateTime.now(clock);
            LocalDate deliveryDate = now.toLocalDate();
            LocalDateTime windowEndedAt = deliveryDate.atTime(properties.cutoffTime());
            SlackDailyDigestRunResult result = digestService.runDaily(
                    deliveryDate,
                    windowEndedAt,
                    now.toLocalDateTime()
            );
            log.info(
                    "Slack Daily Digest 완료: deliveryDate={}, started={}, created={}, ready={}, recovered={}",
                    deliveryDate,
                    result.started(),
                    result.createdCount(),
                    result.readyCount(),
                    result.recoveredCount()
            );
        });
    }

    @Scheduled(
            cron = "${slack.daily-digest.retry-cron:0 */5 * * * *}",
            zone = "${slack.daily-digest.zone:Asia/Seoul}"
    )
    public void runRetry() {
        execute("Daily Digest 보정 및 Delivery 재시도", () -> {
            LocalDateTime now = ZonedDateTime.now(clock)
                    .withSecond(0)
                    .withNano(0)
                    .toLocalDateTime();
            if (!now.toLocalTime().isBefore(properties.cutoffTime())) {
                LocalDate deliveryDate = now.toLocalDate();
                LocalDateTime windowEndedAt = deliveryDate.atTime(properties.cutoffTime());
                SlackDailyDigestRunResult dailyResult = digestService.runDaily(
                        deliveryDate,
                        windowEndedAt,
                        now
                );
                if (dailyResult.started()) {
                    log.info(
                            "Slack Daily Digest 보정 실행 완료: deliveryDate={}, created={}, ready={}, recovered={}",
                            deliveryDate,
                            dailyResult.createdCount(),
                            dailyResult.readyCount(),
                            dailyResult.recoveredCount()
                    );
                    return;
                }
            }
            SlackDeliveryRetryResult result = digestService.runRetry(now);
            log.info(
                    "Slack Delivery 재시도 완료: ready={}, recovered={}",
                    result.readyCount(),
                    result.recoveredCount()
            );
        });
    }

    private void execute(String label, Runnable task) {
        if (!running.compareAndSet(false, true)) {
            log.info("Slack {} 작업이 이미 실행 중이므로 건너뜁니다.", label);
            return;
        }
        try {
            task.run();
        } catch (RuntimeException exception) {
            log.error("Slack {} 작업 실행 실패", label, exception);
        } finally {
            running.set(false);
        }
    }
}
