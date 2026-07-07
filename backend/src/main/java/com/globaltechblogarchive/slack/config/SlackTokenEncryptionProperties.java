package com.globaltechblogarchive.slack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack.bot-token-encryption")
public record SlackTokenEncryptionProperties(
        String keyBase64
) {
}
