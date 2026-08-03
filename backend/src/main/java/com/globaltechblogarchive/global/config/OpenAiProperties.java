package com.globaltechblogarchive.global.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "openai")
@Validated
public record OpenAiProperties(
        @NotBlank String apiKey,
        @DefaultValue("gpt-5-mini") String model,
        @DefaultValue("https://api.openai.com") String baseUrl,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("2m") Duration readTimeout
) {

    public OpenAiProperties {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("openai.connect-timeout must be positive");
        }
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("openai.read-timeout must be positive");
        }
    }
}
