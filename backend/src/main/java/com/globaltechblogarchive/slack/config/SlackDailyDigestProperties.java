package com.globaltechblogarchive.slack.config;

import java.time.Duration;
import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack.daily-digest")
public record SlackDailyDigestProperties(
        String zone,
        LocalTime cutoffTime,
        int maxAttempts,
        Duration retryDelay,
        Duration staleTimeout,
        Duration verificationDelay,
        Duration verificationPermissionErrorDelay,
        int maxVerificationChecks,
        Duration historyLookback,
        int historyPageSize
) {
}
