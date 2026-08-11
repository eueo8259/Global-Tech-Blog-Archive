package com.globaltechblogarchive.slack.application;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SlackMessageLookupClient {

    Optional<SlackMessageLookupResult> findByDeliveryKey(
            String botToken,
            String channelId,
            String deliveryKey,
            LocalDateTime attemptedAt,
            LocalDateTime verificationNow,
            String knownMessageTs
    );
}
