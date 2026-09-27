package com.globaltechblogarchive.slack.application;

import java.time.LocalDateTime;
public interface SlackMessageLookupClient {

    SlackMessageLookupResult findByDeliveryKey(
            String botToken,
            String channelId,
            String deliveryKey,
            LocalDateTime attemptedAt,
            LocalDateTime latestAt,
            String knownMessageTs,
            String cursor
    );
}
