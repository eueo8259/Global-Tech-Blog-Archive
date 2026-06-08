package com.globaltechblogarchive.article.client.openai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiRestClientConfig {

    @Bean
    RestClient openAiRestClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.baseUrl("https://api.openai.com").build();
    }
}
