package com.globaltechblogarchive.slack.application;

import com.globaltechblogarchive.slack.application.digest.SlackChatMessage;

public interface SlackMessageClient {

    SlackMessageSendResult send(String botToken, SlackChatMessage message);
}
