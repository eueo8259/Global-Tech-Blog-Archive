package com.globaltechblogarchive.slack.application.digest;

import java.time.LocalDateTime;

public record ClaimedSlackDelivery(
        Long deliveryId,
        Long channelId,
        String slackChannelId,
        String encryptedBotToken,
        LocalDateTime windowStartedAt,
        LocalDateTime windowEndedAt,
        int attemptCount
) {
}
