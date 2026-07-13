package com.globaltechblogarchive.slack.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.globaltechblogarchive.slack.application.modal.SlackModalView;
import com.globaltechblogarchive.slack.application.modal.SlackModalView.PlainText;
import com.globaltechblogarchive.slack.exception.SlackViewException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackViewRestClientTest {

    @Test
    void openSendsBearerTokenTriggerAndView() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/views.open"))
                .andExpect(header("Authorization", "Bearer xoxb-token"))
                .andExpect(jsonPath("$.trigger_id").value("trigger-123"))
                .andExpect(jsonPath("$.view.callback_id").value("subscription_settings"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
        SlackViewRestClient client = new SlackViewRestClient(builder.build());

        client.open("xoxb-token", "trigger-123", view());

        server.verify();
    }

    @Test
    void openMapsSlackErrorResponseToViewException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://slack.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/views.open"))
                .andRespond(withSuccess(
                        "{\"ok\":false,\"error\":\"expired_trigger_id\"}",
                        MediaType.APPLICATION_JSON
                ));
        SlackViewRestClient client = new SlackViewRestClient(builder.build());

        assertThatThrownBy(() -> client.open("xoxb-token", "trigger-123", view()))
                .isInstanceOfSatisfying(SlackViewException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                                .isEqualTo("Slack views.open failed: expired_trigger_id"));
        server.verify();
    }

    private SlackModalView view() {
        PlainText text = new PlainText("plain_text", "text");
        return new SlackModalView(
                "modal",
                "subscription_settings",
                text,
                text,
                text,
                "{}",
                List.of()
        );
    }
}
