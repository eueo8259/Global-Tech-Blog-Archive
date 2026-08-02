package com.globaltechblogarchive.crawl.scheduler;

import com.globaltechblogarchive.crawl.application.ArticleAiReviewService;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "crawl.ai-review.enabled", havingValue = "true")
public class ArticleAiReviewScheduler {

    private final ArticleAiReviewService reviewService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(
            cron = "${crawl.ai-review.cron:0 */5 * * * *}",
            zone = "${crawl.ai-review.zone:Asia/Seoul}"
    )
    public void runScheduledReview() {
        if (!running.compareAndSet(false, true)) {
            log.info("Scheduled article AI review already running, skipping.");
            return;
        }
        try {
            AiReviewRunResult result = reviewService.runScheduled();
            log.info(
                    "Scheduled article AI review completed: candidateCount={}, approvedCount={}, "
                            + "rejectedCount={}, failedCount={}, retryWaitingCount={}, storedCount={}, recoveredCount={}",
                    result.candidateCount(),
                    result.approvedCount(),
                    result.rejectedCount(),
                    result.failedCount(),
                    result.retryWaitingCount(),
                    result.storedCount(),
                    result.recoveredCount()
            );
        } catch (RuntimeException exception) {
            log.error("Scheduled article AI review failed", exception);
        } finally {
            running.set(false);
        }
    }
}
