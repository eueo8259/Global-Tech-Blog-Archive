package com.globaltechblogarchive.slack.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.globaltechblogarchive.slack.application.SlackMessageLookupResult;
import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import com.globaltechblogarchive.slack.exception.SlackMessageLookupException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackHistoryRestClientTest {

    private static final LocalDateTime ATTEMPTED_AT = LocalDateTime.of(2026, 8, 11, 9, 0);
    private static final LocalDateTime VERIFICATION_NOW = ATTEMPTED_AT.plusMinutes(5);

    @Test
    void findByDeliveryKeySendsRequiredParametersAndReturnsMatchingMessage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/conversations.history")))
                .andExpect(header("Authorization", "Bearer xoxb-token"))
                .andExpect(queryParam("channel", "C123"))
                .andExpect(queryParam("include_all_metadata", "true"))
                .andExpect(queryParam("inclusive", "true"))
                .andExpect(queryParam("limit", "15"))
                .andExpect(queryParam("oldest", org.hamcrest.Matchers.notNullValue()))
                .andExpect(queryParam("latest", org.hamcrest.Matchers.notNullValue()))
                .andRespond(withSuccess(responseWithMessage(
                        "techport_digest_sent",
                        "delivery-key",
                        "1720937160.000100",
                        false,
                        ""
                ), MediaType.APPLICATION_JSON));
        SlackHistoryRestClient client = new SlackHistoryRestClient(builder.build(), properties());

        SlackMessageLookupResult result = client.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                ATTEMPTED_AT,
                VERIFICATION_NOW,
                null,
                null
        );

        assertThat(result).isEqualTo(SlackMessageLookupResult.found("1720937160.000100"));
        server.verify();
    }

    @Test
    void findByDeliveryKeyIgnoresDifferentMetadata() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/conversations.history")))
                .andRespond(withSuccess(responseWithMessage(
                        "different_event",
                        "delivery-key",
                        "1720937160.000100",
                        false,
                        ""
                ), MediaType.APPLICATION_JSON));
        SlackHistoryRestClient client = new SlackHistoryRestClient(builder.build(), properties());

        SlackMessageLookupResult result = client.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                ATTEMPTED_AT,
                VERIFICATION_NOW,
                null,
                null
        );

        assertThat(result).isEqualTo(SlackMessageLookupResult.notFound(null));
        server.verify();
    }

    @Test
    void findByDeliveryKeyIgnoresDifferentDeliveryKey() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/conversations.history")))
                .andRespond(withSuccess(responseWithMessage(
                        "techport_digest_sent",
                        "different-key",
                        "1720937160.000100",
                        false,
                        ""
                ), MediaType.APPLICATION_JSON));
        SlackHistoryRestClient client = new SlackHistoryRestClient(builder.build(), properties());

        SlackMessageLookupResult result = client.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                ATTEMPTED_AT,
                VERIFICATION_NOW,
                null,
                null
        );

        assertThat(result).isEqualTo(SlackMessageLookupResult.notFound(null));
        server.verify();
    }

    @Test
    void findByDeliveryKeyReturnsCursorWithoutCallingNextPage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/conversations.history")))
                .andRespond(withSuccess(
                        "{\"ok\":true,\"messages\":[],\"has_more\":true,"
                                + "\"response_metadata\":{\"next_cursor\":\"cursor-2\"}}",
                        MediaType.APPLICATION_JSON
                ));
        SlackHistoryRestClient client = new SlackHistoryRestClient(builder.build(), properties());

        SlackMessageLookupResult result = client.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                ATTEMPTED_AT,
                VERIFICATION_NOW,
                null,
                null
        );

        assertThat(result).isEqualTo(SlackMessageLookupResult.notFound("cursor-2"));
        server.verify();
    }

    @Test
    void findByDeliveryKeyResumesFromCursorOnLaterInvocation() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/conversations.history")))
                .andExpect(queryParam("cursor", "cursor-2"))
                .andRespond(withSuccess(responseWithMessage(
                        "techport_digest_sent",
                        "delivery-key",
                        "1720937160.000200",
                        false,
                        ""
                ), MediaType.APPLICATION_JSON));
        SlackHistoryRestClient client = new SlackHistoryRestClient(builder.build(), properties());

        SlackMessageLookupResult result = client.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                ATTEMPTED_AT,
                VERIFICATION_NOW,
                null,
                "cursor-2"
        );

        assertThat(result).isEqualTo(SlackMessageLookupResult.found("1720937160.000200"));
        server.verify();
    }

    @Test
    void findByDeliveryKeyChecksKnownTimestampFirst() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/conversations.history")))
                .andExpect(queryParam("oldest", "1720937160.000100"))
                .andExpect(queryParam("latest", "1720937160.000100"))
                .andExpect(queryParam("limit", "1"))
                .andRespond(withSuccess(
                        "{\"ok\":true,\"messages\":[{\"ts\":\"1720937160.000100\"}],"
                                + "\"has_more\":false}",
                        MediaType.APPLICATION_JSON
                ));
        SlackHistoryRestClient client = new SlackHistoryRestClient(builder.build(), properties());

        SlackMessageLookupResult result = client.findByDeliveryKey(
                "xoxb-token",
                "C123",
                "delivery-key",
                ATTEMPTED_AT,
                VERIFICATION_NOW,
                "1720937160.000100",
                null
        );

        assertThat(result).isEqualTo(SlackMessageLookupResult.found("1720937160.000100"));
        server.verify();
    }

    private String responseWithMessage(
            String eventType,
            String deliveryKey,
            String ts,
            boolean hasMore,
            String nextCursor
    ) {
        return "{\"ok\":true,\"messages\":[{\"ts\":\"" + ts + "\",\"metadata\":{"
                + "\"event_type\":\"" + eventType + "\",\"event_payload\":{"
                + "\"delivery_key\":\"" + deliveryKey + "\"}}}],"
                + "\"has_more\":" + hasMore + ",\"response_metadata\":{"
                + "\"next_cursor\":\"" + nextCursor + "\"}}";
    }

    private SlackDailyDigestProperties properties() {
        return new SlackDailyDigestProperties(
                "Asia/Seoul",
                LocalTime.of(9, 0),
                3,
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                Duration.ofMinutes(5),
                Duration.ofHours(1),
                3,
                Duration.ofMinutes(1),
                15
        );
    }
}
