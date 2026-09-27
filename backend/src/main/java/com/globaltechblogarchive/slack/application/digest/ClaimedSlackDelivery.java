package com.globaltechblogarchive.slack.application.digest;

import java.time.LocalDateTime;

public record ClaimedSlackDelivery(
        Long deliveryId,
        Long channelId,
        String slackChannelId,
        String encryptedBotToken,
        String deliveryKey,
        LocalDateTime windowStartedAt,
        LocalDateTime windowEndedAt,
        int attemptCount
) {
}
