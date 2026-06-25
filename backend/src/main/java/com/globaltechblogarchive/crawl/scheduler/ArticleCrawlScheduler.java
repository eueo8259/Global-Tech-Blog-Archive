package com.globaltechblogarchive.crawl.scheduler;

import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "crawl.scheduler.enabled", havingValue = "true")
public class ArticleCrawlScheduler {

    private final ArticleCrawlService articleCrawlService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(
            cron = "${crawl.scheduler.cron:0 0 3 * * *}",
            zone = "${crawl.scheduler.zone:Asia/Seoul}"
    )
    public void runScheduledCrawl() {
        if (!running.compareAndSet(false, true)) {
            log.info("Scheduled article crawl already running, skipping.");
            return;
        }

        Instant startedAt = Instant.now();
        log.info("Scheduled article crawl started.");
        try {
            ArticleCrawlResult result = articleCrawlService.runScheduled();
            log.info(
                    "Scheduled article crawl completed: elapsedMs={}, sourceCount={}, successCount={}, "
                            + "failureCount={}, candidateCount={}, storedCount={}, aiApprovedCount={}, "
                            + "aiRejectedCount={}, aiFailedCount={}",
                    elapsedMillis(startedAt),
                    result.sourceCount(),
                    result.successCount(),
                    result.failureCount(),
                    result.candidateCount(),
                    result.storedCount(),
                    result.aiApprovedCount(),
                    result.aiRejectedCount(),
                    result.aiFailedCount()
            );
        } catch (RuntimeException exception) {
            log.error("Scheduled article crawl failed: elapsedMs={}", elapsedMillis(startedAt), exception);
        } finally {
            running.set(false);
        }
    }

    private long elapsedMillis(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
