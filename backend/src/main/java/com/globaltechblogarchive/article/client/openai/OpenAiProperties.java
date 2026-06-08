package com.globaltechblogarchive.article.client.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String model
) {

    private static final String DEFAULT_MODEL = "gpt-5-mini";

    public String modelOrDefault() {
        if (model == null || model.isBlank()) {
            return DEFAULT_MODEL;
        }
        return model;
    }
}
