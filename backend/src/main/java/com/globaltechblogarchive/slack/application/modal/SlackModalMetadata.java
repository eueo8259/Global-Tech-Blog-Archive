package com.globaltechblogarchive.slack.application.modal;

public record SlackModalMetadata(
        String teamId,
        String channelId,
        String channelName
) {
}
