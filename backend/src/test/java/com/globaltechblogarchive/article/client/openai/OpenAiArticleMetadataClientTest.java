package com.globaltechblogarchive.article.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiRequestException;
import com.globaltechblogarchive.global.config.OpenAiProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiArticleMetadataClientTest {

    @Test
    void decideClassifiesServerErrorAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses")).andRespond(withServerError());
        OpenAiArticleMetadataClient client = new OpenAiArticleMetadataClient(
                builder.baseUrl("https://api.openai.com").build(),
                new OpenAiProperties("test-key", "gpt-5-mini", "https://api.openai.com"),
                new OpenAiArticleMetadataRequestFactory(new ObjectMapper()),
                new OpenAiArticleMetadataResponseParser(new ObjectMapper())
        );

        assertThatThrownBy(() -> client.decide(List.of(new ArticleMetadataInput(0, "Title", "Context"))))
                .isInstanceOfSatisfying(ArticleMetadataAiRequestException.class, exception -> {
                    assertThat(exception.getFailureCode()).isEqualTo("OPENAI_HTTP_500");
                    assertThat(exception.isRetryable()).isTrue();
                });

        server.verify();
    }
}
