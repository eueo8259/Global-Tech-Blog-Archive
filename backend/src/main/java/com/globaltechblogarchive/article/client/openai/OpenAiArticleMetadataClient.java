package com.globaltechblogarchive.article.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;

import java.util.List;

import com.globaltechblogarchive.global.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenAiArticleMetadataClient implements ArticleMetadataAiClient {

    private static final String RESPONSES_PATH = "/v1/responses";

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final OpenAiArticleMetadataRequestFactory requestFactory;
    private final OpenAiArticleMetadataResponseParser responseParser;

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public List<ArticleMetadataDecision> decide(List<ArticleMetadataInput> inputs) {
        JsonNode request = requestFactory.create(inputs, model());
        String responseBody = restClient.post()
                .uri(RESPONSES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(properties.apiKey()))
                .body(request)
                .retrieve()
                .body(String.class);
        return responseParser.parse(responseBody);
    }
}
