package com.globaltechblogarchive.global.config;

import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiRestClientConfig {

    @Bean
    public RestClient openAiRestClient(
            RestClient.Builder restClientBuilder,
            OpenAiProperties properties,
            AiReviewProperties aiReviewProperties
    ) {
        validateTimeouts(properties, aiReviewProperties);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private void validateTimeouts(OpenAiProperties properties, AiReviewProperties aiReviewProperties) {
        if (properties.connectTimeout().compareTo(aiReviewProperties.staleTimeout()) >= 0) {
            throw new IllegalArgumentException("openai.connect-timeout must be shorter than crawl.ai-review.stale-timeout");
        }
        if (properties.readTimeout().compareTo(aiReviewProperties.staleTimeout()) >= 0) {
            throw new IllegalArgumentException("openai.read-timeout must be shorter than crawl.ai-review.stale-timeout");
        }
    }
}
