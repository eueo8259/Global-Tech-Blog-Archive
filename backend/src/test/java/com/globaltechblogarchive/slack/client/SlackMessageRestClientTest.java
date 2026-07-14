package com.globaltechblogarchive.slack.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.globaltechblogarchive.slack.application.SlackMessageSendResult;
import com.globaltechblogarchive.slack.application.digest.SlackChatMessage;
import com.globaltechblogarchive.slack.application.digest.SlackChatMessage.Block;
import com.globaltechblogarchive.slack.exception.SlackMessageSendException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackMessageRestClientTest {

    @Test
    void sendPostsOneMessageAndReturnsSlackTimestamp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/chat.postMessage"))
                .andExpect(header("Authorization", "Bearer xoxb-token"))
                .andExpect(jsonPath("$.channel").value("C123"))
                .andExpect(jsonPath("$.blocks[0].type").value("header"))
                .andRespond(withSuccess(
                        "{\"ok\":true,\"ts\":\"1720937160.000100\"}",
                        MediaType.APPLICATION_JSON
                ));
        SlackMessageRestClient client = new SlackMessageRestClient(builder.build());

        SlackMessageSendResult result = client.send("xoxb-token", message());

        assertThat(result.messageTs()).isEqualTo("1720937160.000100");
        server.verify();
    }

    @Test
    void sendClassifiesSlackPermanentError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/chat.postMessage"))
                .andRespond(withSuccess(
                        "{\"ok\":false,\"error\":\"invalid_auth\"}",
                        MediaType.APPLICATION_JSON
                ));
        SlackMessageRestClient client = new SlackMessageRestClient(builder.build());

        assertThatThrownBy(() -> client.send("xoxb-token", message()))
                .isInstanceOfSatisfying(SlackMessageSendException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("invalid_auth");
                    assertThat(exception.isRetryable()).isFalse();
                });
        server.verify();
    }

    @Test
    void sendUsesRetryAfterForRateLimit() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/chat.postMessage"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header(HttpHeaders.RETRY_AFTER, "30"));
        SlackMessageRestClient client = new SlackMessageRestClient(builder.build());

        assertThatThrownBy(() -> client.send("xoxb-token", message()))
                .isInstanceOfSatisfying(SlackMessageSendException.class, exception -> {
                    assertThat(exception.isRetryable()).isTrue();
                    assertThat(exception.getRetryAfter()).isEqualTo(Duration.ofSeconds(30));
                });
        server.verify();
    }

    @Test
    void sendClassifiesServerErrorAsRetryable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/chat.postMessage"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        SlackMessageRestClient client = new SlackMessageRestClient(builder.build());

        assertThatThrownBy(() -> client.send("xoxb-token", message()))
                .isInstanceOfSatisfying(SlackMessageSendException.class, exception ->
                        assertThat(exception.isRetryable()).isTrue());
        server.verify();
    }

    private SlackChatMessage message() {
        return new SlackChatMessage(
                "C123",
                "오늘의 새로운 기술 아티클 1개",
                List.of(Block.header("오늘의 새로운 기술 아티클 1개"))
        );
    }
}
