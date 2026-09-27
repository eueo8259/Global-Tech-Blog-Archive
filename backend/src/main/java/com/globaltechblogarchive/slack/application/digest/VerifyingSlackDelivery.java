package com.globaltechblogarchive.slack.application.digest;

import java.time.LocalDateTime;

public record VerifyingSlackDelivery(
        Long deliveryId,
        String slackChannelId,
        String encryptedBotToken,
        String deliveryKey,
        LocalDateTime attemptedAt,
        String knownMessageTs,
        String historyCursor,
        LocalDateTime historyLatestAt
) {
}
