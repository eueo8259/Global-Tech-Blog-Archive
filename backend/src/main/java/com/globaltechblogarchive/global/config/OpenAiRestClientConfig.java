package com.globaltechblogarchive.global.config;

import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import java.net.http.HttpClient;
import java.time.Duration;
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
        int maximumClaimSize = Math.max(
                aiReviewProperties.claimLimit(),
                aiReviewProperties.maxFailureRetryLimit()
        );
        long batchCount = (maximumClaimSize + (long) aiReviewProperties.batchSize() - 1)
                / aiReviewProperties.batchSize();
        Duration maximumWorkloadDuration = properties.connectTimeout()
                .plus(properties.readTimeout())
                .multipliedBy(batchCount);
        if (maximumWorkloadDuration.compareTo(aiReviewProperties.staleTimeout()) >= 0) {
            throw new IllegalArgumentException(
                    "maximum OpenAI claim workload must be shorter than crawl.ai-review.stale-timeout"
            );
        }
    }
}
