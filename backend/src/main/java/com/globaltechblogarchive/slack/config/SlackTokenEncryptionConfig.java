package com.globaltechblogarchive.slack.config;

import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SlackTokenEncryptionConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
    SlackTokenEncryptor slackTokenEncryptor(SlackTokenEncryptionProperties properties) {
        return new SlackTokenEncryptor(properties.keyBase64());
    }
}
