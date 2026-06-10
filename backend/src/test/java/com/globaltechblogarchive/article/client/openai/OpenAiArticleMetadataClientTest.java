package com.globaltechblogarchive.article.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiArticleMetadataClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiArticleMetadataRequestFactory requestFactory =
            new OpenAiArticleMetadataRequestFactory(objectMapper);
    private final OpenAiArticleMetadataResponseParser responseParser =
            new OpenAiArticleMetadataResponseParser(objectMapper);
    private final OpenAiArticleMetadataClient client = new OpenAiArticleMetadataClient(
            (RestClient) null,
            new OpenAiProperties("test-key", "gpt-5-mini"),
            requestFactory,
            responseParser
    );

    @Test
    void modelReturnsConfiguredModel() {
        assertThat(client.model()).isEqualTo("gpt-5-mini");
    }

    @Test
    void decideRejectsBlankApiKeyBeforeHttpCall() {
        OpenAiArticleMetadataClient blankKeyClient = new OpenAiArticleMetadataClient(
                (RestClient) null,
                new OpenAiProperties("", "gpt-5-mini"),
                requestFactory,
                responseParser
        );

        assertThatThrownBy(() -> blankKeyClient.decide(List.of(new ArticleMetadataInput(0, "Scaling APIs"))))
                .isInstanceOf(ArticleMetadataAiClientException.class)
                .hasMessage("OPENAI_API_KEY is required");
    }
}
