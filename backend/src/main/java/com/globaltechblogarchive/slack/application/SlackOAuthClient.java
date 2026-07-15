package com.globaltechblogarchive.slack.application;

public interface SlackOAuthClient {

    SlackOAuthInstallation exchangeCode(String code);

    record SlackOAuthInstallation(
            String teamId,
            String teamName,
            String accessToken,
            String botUserId,
            String scope
    ) {
    }
}
