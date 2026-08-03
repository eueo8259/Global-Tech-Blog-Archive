package com.globaltechblogarchive.article.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiRequestException;
import com.globaltechblogarchive.global.config.OpenAiProperties;
import com.globaltechblogarchive.global.config.OpenAiRestClientConfig;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
                new OpenAiProperties(
                        "test-key",
                        "gpt-5-mini",
                        "https://api.openai.com",
                        Duration.ofSeconds(3),
                        Duration.ofMinutes(2)
                ),
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

    @Test
    void decideClassifiesRequestTimeoutAsRetryable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withStatus(HttpStatus.REQUEST_TIMEOUT));
        OpenAiProperties properties = new OpenAiProperties(
                "test-key",
                "gpt-5-mini",
                "https://api.openai.com",
                Duration.ofSeconds(3),
                Duration.ofMinutes(2)
        );
        OpenAiArticleMetadataClient client = new OpenAiArticleMetadataClient(
                builder.baseUrl(properties.baseUrl()).build(),
                properties,
                new OpenAiArticleMetadataRequestFactory(new ObjectMapper()),
                new OpenAiArticleMetadataResponseParser(new ObjectMapper())
        );

        assertThatThrownBy(() -> client.decide(List.of(new ArticleMetadataInput(0, "Title", "Context"))))
                .isInstanceOfSatisfying(ArticleMetadataAiRequestException.class, exception -> {
                    assertThat(exception.getFailureCode()).isEqualTo("OPENAI_HTTP_408");
                    assertThat(exception.isRetryable()).isTrue();
                });

        server.verify();
    }

    @Test
    void decideClassifiesReadTimeoutAsRetryable() throws IOException {
        HttpServer server = delayedServer(Duration.ofMillis(500));
        server.start();
        try {
            OpenAiProperties properties = new OpenAiProperties(
                    "test-key",
                    "gpt-5-mini",
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofMillis(100),
                    Duration.ofMillis(100)
            );
            AiReviewProperties aiReviewProperties = aiReviewProperties(Duration.ofSeconds(3));
            RestClient restClient = new OpenAiRestClientConfig().openAiRestClient(
                    RestClient.builder(),
                    properties,
                    aiReviewProperties
            );
            OpenAiArticleMetadataClient client = new OpenAiArticleMetadataClient(
                    restClient,
                    properties,
                    new OpenAiArticleMetadataRequestFactory(new ObjectMapper()),
                    new OpenAiArticleMetadataResponseParser(new ObjectMapper())
            );

            assertThatThrownBy(() -> client.decide(List.of(new ArticleMetadataInput(0, "Title", "Context"))))
                    .isInstanceOfSatisfying(ArticleMetadataAiRequestException.class, exception -> {
                        assertThat(exception.getFailureCode()).isEqualTo("OPENAI_NETWORK_ERROR");
                        assertThat(exception.isRetryable()).isTrue();
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void restClientRejectsClaimWorkloadLongerThanStaleTimeout() {
        OpenAiProperties properties = new OpenAiProperties(
                "test-key",
                "gpt-5-mini",
                "https://api.openai.com",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );

        assertThatThrownBy(() -> new OpenAiRestClientConfig().openAiRestClient(
                RestClient.builder(),
                properties,
                aiReviewProperties(Duration.ofSeconds(2))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximum OpenAI claim workload must be shorter than crawl.ai-review.stale-timeout");
    }

    private HttpServer delayedServer(Duration delay) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            try {
                Thread.sleep(delay);
                exchange.sendResponseHeaders(200, 0);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        return server;
    }

    private AiReviewProperties aiReviewProperties(Duration staleTimeout) {
        return new AiReviewProperties(
                50,
                10,
                3,
                100,
                Duration.ofMinutes(5),
                staleTimeout
        );
    }
}
