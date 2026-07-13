package com.globaltechblogarchive.slack.application;

public record SlackCommandResult(
        boolean succeeded,
        String userMessage
) {

    public static SlackCommandResult success() {
        return new SlackCommandResult(true, null);
    }

    public static SlackCommandResult failure(String userMessage) {
        return new SlackCommandResult(false, userMessage);
    }
}
