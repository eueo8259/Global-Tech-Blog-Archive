package com.globaltechblogarchive.article.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiArticleMetadataClient implements ArticleMetadataAiClient {

    private static final String RESPONSES_PATH = "/v1/responses";

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final OpenAiArticleMetadataRequestFactory requestFactory;
    private final OpenAiArticleMetadataResponseParser responseParser;

    public OpenAiArticleMetadataClient(
            RestClient openAiRestClient,
            OpenAiProperties properties,
            OpenAiArticleMetadataRequestFactory requestFactory,
            OpenAiArticleMetadataResponseParser responseParser
    ) {
        this.restClient = openAiRestClient;
        this.properties = properties;
        this.requestFactory = requestFactory;
        this.responseParser = responseParser;
    }

    @Override
    public String model() {
        return properties.modelOrDefault();
    }

    @Override
    public List<ArticleMetadataDecision> decide(List<ArticleMetadataInput> inputs) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new ArticleMetadataAiClientException("OPENAI_API_KEY is required");
        }
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
