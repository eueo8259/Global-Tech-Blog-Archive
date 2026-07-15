package com.globaltechblogarchive.slack.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.global.error.GlobalExceptionHandler;
import com.globaltechblogarchive.slack.api.dto.SlackSlashCommand;
import com.globaltechblogarchive.slack.application.SlackCommandResult;
import com.globaltechblogarchive.slack.application.SlackCommandService;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.filter.SlackRequestSignatureFilter;
import com.globaltechblogarchive.slack.support.SlackFormPayloadParser;
import com.globaltechblogarchive.slack.support.SlackRequestSignatureVerifier;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SlackCommandControllerTest {

    private static final String SIGNING_SECRET = "signing-secret";
    private static final String BODY = "command=%2Fsubscribe&team_id=T123&channel_id=C123"
            + "&channel_name=tech-news&user_id=U123&trigger_id=trigger-123";

    private SlackCommandService commandService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(SlackCommandService.class);
        SlackProperties properties = new SlackProperties(
                "client-id",
                "client-secret",
                SIGNING_SECRET,
                "http://localhost/api/slack/oauth/callback",
                "commands",
                "http://localhost"
        );
        SlackRequestSignatureFilter filter = new SlackRequestSignatureFilter(
                new SlackRequestSignatureVerifier(properties)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SlackCommandController(commandService, new SlackFormPayloadParser())
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @Test
    void commandParsesFormBodyAfterSignatureVerification() throws Exception {
        when(commandService.openSubscriptionModal(org.mockito.ArgumentMatchers.any()))
                .thenReturn(SlackCommandResult.success());
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/slack/commands")
                        .servletPath("/api/slack/commands")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", signature(timestamp, BODY))
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(commandService).openSubscriptionModal(new SlackSlashCommand(
                "/subscribe", "T123", "C123", "tech-news", "U123", "trigger-123"
        ));
    }

    @Test
    void commandReturnsEphemeralMessageForBusinessFailure() throws Exception {
        when(commandService.openSubscriptionModal(org.mockito.ArgumentMatchers.any()))
                .thenReturn(SlackCommandResult.failure("구독 설정 화면을 열지 못했습니다."));
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/slack/commands")
                        .servletPath("/api/slack/commands")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", signature(timestamp, BODY))
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_type").value("ephemeral"))
                .andExpect(jsonPath("$.text").value("구독 설정 화면을 열지 못했습니다."));
    }

    @Test
    void commandRejectsUnsupportedCommand() throws Exception {
        String body = BODY.replace("%2Fsubscribe", "%2Funknown");
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/slack/commands")
                        .servletPath("/api/slack/commands")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", signature(timestamp, body))
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported Slack command"));
    }

    @Test
    void commandRejectsUnsupportedContentType() throws Exception {
        String body = "{}";
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/slack/commands")
                        .servletPath("/api/slack/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Slack-Request-Timestamp", timestamp)
                        .header("X-Slack-Signature", signature(timestamp, body))
                        .content(body))
                .andExpect(status().isUnsupportedMediaType());
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(("v0:" + timestamp + ":" + body).getBytes(StandardCharsets.UTF_8));
        return "v0=" + HexFormat.of().formatHex(digest);
    }
}
