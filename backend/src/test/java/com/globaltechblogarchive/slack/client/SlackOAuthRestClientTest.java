package com.globaltechblogarchive.slack.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.slack.application.SlackOAuthClient.SlackOAuthInstallation;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.exception.SlackOAuthException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackOAuthRestClientTest {

    @Test
    void exchangeCodeReturnsInstallation() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/oauth.v2.access"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "ok": true,
                          "access_token": "xoxb-token",
                          "scope": "commands,chat:write",
                          "bot_user_id": "B123",
                          "team": {
                            "id": "T123",
                            "name": "TechPort"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        SlackOAuthRestClient client = new SlackOAuthRestClient(builder, properties());

        SlackOAuthInstallation installation = client.exchangeCode("code-123");

        assertThat(installation.teamId()).isEqualTo("T123");
        assertThat(installation.teamName()).isEqualTo("TechPort");
        assertThat(installation.accessToken()).isEqualTo("xoxb-token");
        assertThat(installation.botUserId()).isEqualTo("B123");
        assertThat(installation.scope()).isEqualTo("commands,chat:write");
        server.verify();
    }

    @Test
    void exchangeCodeMapsSlackErrorResponseToOAuthException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/oauth.v2.access"))
                .andRespond(withSuccess("""
                        {
                          "ok": false,
                          "error": "invalid_code"
                        }
                        """, MediaType.APPLICATION_JSON));
        SlackOAuthRestClient client = new SlackOAuthRestClient(builder, properties());

        assertThatThrownBy(() -> client.exchangeCode("bad-code"))
                .isInstanceOfSatisfying(SlackOAuthException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SLACK_OAUTH_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("Slack OAuth failed: invalid_code");
                });

        server.verify();
    }

    @Test
    void exchangeCodeMapsHttpFailureToOAuthException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://slack.com/api/oauth.v2.access")).andRespond(withServerError());
        SlackOAuthRestClient client = new SlackOAuthRestClient(builder, properties());

        assertThatThrownBy(() -> client.exchangeCode("code-123"))
                .isInstanceOfSatisfying(SlackOAuthException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SLACK_OAUTH_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("Slack OAuth token exchange failed");
                });

        server.verify();
    }

    private SlackProperties properties() {
        return new SlackProperties(
                "client-id",
                "client-secret",
                "http://localhost:8080/slack/oauth/callback",
                "commands,chat:write",
                "http://localhost:5173"
        );
    }
}
