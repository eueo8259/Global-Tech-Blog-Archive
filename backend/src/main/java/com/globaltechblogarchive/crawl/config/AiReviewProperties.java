package com.globaltechblogarchive.crawl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crawl.ai-review")
public record AiReviewProperties(
        int claimLimit,
        int batchSize,
        int maxAttempts,
        int maxFailureRetryLimit,
        Duration retryDelay,
        Duration staleTimeout
) {

    public AiReviewProperties {
        if (claimLimit < 1) {
            throw new IllegalArgumentException("crawl.ai-review.claim-limit must be greater than 0");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("crawl.ai-review.batch-size must be greater than 0");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("crawl.ai-review.max-attempts must be greater than 0");
        }
        if (maxFailureRetryLimit < 1) {
            throw new IllegalArgumentException("crawl.ai-review.max-failure-retry-limit must be greater than 0");
        }
        if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()) {
            throw new IllegalArgumentException("crawl.ai-review.retry-delay must be positive");
        }
        if (staleTimeout == null || staleTimeout.isNegative() || staleTimeout.isZero()) {
            throw new IllegalArgumentException("crawl.ai-review.stale-timeout must be positive");
        }
    }
}
