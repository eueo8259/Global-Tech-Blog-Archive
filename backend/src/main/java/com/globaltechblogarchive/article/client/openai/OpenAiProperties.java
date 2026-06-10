package com.globaltechblogarchive.article.client.openai;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "openai")
@Validated
public record OpenAiProperties(
        @NotBlank String apiKey,
        @DefaultValue("gpt-5-mini") String model,
        @DefaultValue("https://api.openai.com") String baseUrl
) {

}
