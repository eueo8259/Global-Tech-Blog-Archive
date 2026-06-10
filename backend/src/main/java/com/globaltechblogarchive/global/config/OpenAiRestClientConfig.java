package com.globaltechblogarchive.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiRestClientConfig {

    @Bean
    RestClient openAiRestClient(RestClient.Builder restClientBuilder, OpenAiProperties properties) {
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }
}
