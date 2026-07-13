package com.globaltechblogarchive.slack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
        String clientId,
        String clientSecret,
        String signingSecret,
        String redirectUri,
        String botScopes,
        String settingsBaseUrl
) {
}
