package com.globaltechblogarchive.slack.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.application.SlackCommandResult;
import com.globaltechblogarchive.slack.application.SlackCommandService;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.filter.SlackRequestSignatureFilter;
import com.globaltechblogarchive.slack.support.SlackFormPayloadParser;
import com.globaltechblogarchive.slack.support.SlackRequestSignatureVerifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        classes = SlackCommandHttpIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "slack.bot-token-encryption.key-base64=dGVzdA=="
)
class SlackCommandHttpIntegrationTest {

    private static final String SIGNING_SECRET = "signing-secret";
    private static final String BODY = "command=%2Fsubscribe&team_id=T123&channel_id=C123"
            + "&channel_name=tech-news&user_id=U123&trigger_id=trigger-123";

    @LocalServerPort
    private int port;

    @Autowired
    private SlackCommandService commandService;

    @Test
    void commandParsesSignedFormRequestThroughEmbeddedTomcat() throws Exception {
        when(commandService.openSubscriptionModal(any())).thenReturn(SlackCommandResult.success());
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/slack/commands"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Slack-Request-Timestamp", timestamp)
                .header("X-Slack-Signature", signature(timestamp, BODY))
                .POST(HttpRequest.BodyPublishers.ofString(BODY))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        verify(commandService).openSubscriptionModal(any());
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(("v0:" + timestamp + ":" + body).getBytes(StandardCharsets.UTF_8));
        return "v0=" + HexFormat.of().formatHex(digest);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration"
    })
    @Import({
            SlackCommandController.class,
            SlackRequestSignatureFilter.class,
            SlackRequestSignatureVerifier.class,
            SlackFormPayloadParser.class
    })
    static class TestApplication {

        @Bean
        SlackCommandService slackCommandService() {
            return mock(SlackCommandService.class);
        }

        @Bean
        SlackProperties slackProperties() {
            return new SlackProperties(
                    "client-id",
                    "client-secret",
                    SIGNING_SECRET,
                    "http://localhost/api/slack/oauth/callback",
                    "commands",
                    "http://localhost"
            );
        }
    }
}
