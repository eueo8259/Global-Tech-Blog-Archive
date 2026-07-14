package com.globaltechblogarchive.slack.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.GlobalExceptionHandler;
import com.globaltechblogarchive.slack.api.dto.SlackViewSubmission;
import com.globaltechblogarchive.slack.application.SlackSubscriptionCommandService;
import com.globaltechblogarchive.slack.config.SlackProperties;
import com.globaltechblogarchive.slack.exception.InvalidSlackCompanySelectionException;
import com.globaltechblogarchive.slack.filter.SlackRequestSignatureFilter;
import com.globaltechblogarchive.slack.support.SlackRequestSignatureVerifier;
import java.net.URLEncoder;
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

class SlackInteractivityControllerTest {

    private static final String SIGNING_SECRET = "signing-secret";

    private SlackSubscriptionCommandService commandService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(SlackSubscriptionCommandService.class);
        objectMapper = new ObjectMapper();
        SlackProperties properties = new SlackProperties(
                "client-id", "client-secret", SIGNING_SECRET,
                "http://localhost/api/slack/oauth/callback", "commands", "http://localhost"
        );
        SlackRequestSignatureFilter filter = new SlackRequestSignatureFilter(
                new SlackRequestSignatureVerifier(properties)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SlackInteractivityController(commandService, objectMapper)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @Test
    void interactivityVerifiesSignatureAndUpdatesSubscriptions() throws Exception {
        String body = formBody(validPayload());
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(signedRequest(body, timestamp, MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(commandService).updateSubscriptions(any(SlackViewSubmission.class));
    }

    @Test
    void interactivityRejectsMalformedPayload() throws Exception {
        String body = formBody("{");
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(signedRequest(body, timestamp, MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isBadRequest());
    }

    @Test
    void interactivityRejectsJsonContentType() throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(signedRequest("{}", timestamp, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void interactivityReturnsBlockErrorForUnknownCompany() throws Exception {
        doThrow(new InvalidSlackCompanySelectionException(ErrorCode.INVALID_INPUT_VALUE, "unknown company"))
                .when(commandService).updateSubscriptions(any());
        String body = formBody(validPayload());
        String timestamp = Long.toString(Instant.now().getEpochSecond());

        mockMvc.perform(signedRequest(body, timestamp, MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_action").value("errors"))
                .andExpect(jsonPath("$.errors.company_subscriptions")
                        .value("존재하지 않는 회사가 포함되어 있습니다. 다시 시도해 주세요."))
                .andExpect(jsonPath("$.errors.selected_companies").doesNotExist());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signedRequest(
            String body,
            String timestamp,
            MediaType contentType
    ) throws Exception {
        return post("/api/slack/interactivity")
                .servletPath("/api/slack/interactivity")
                .contentType(contentType)
                .header("X-Slack-Request-Timestamp", timestamp)
                .header("X-Slack-Signature", signature(timestamp, body))
                .content(body);
    }

    private String formBody(String payload) {
        return "payload=" + URLEncoder.encode(payload, StandardCharsets.UTF_8);
    }

    private String validPayload() throws Exception {
        var root = objectMapper.createObjectNode();
        root.put("type", "view_submission");
        root.putObject("team").put("id", "T123");
        var view = root.putObject("view");
        view.put("callback_id", "subscription_settings");
        view.put(
                "private_metadata",
                objectMapper.writeValueAsString(new TestMetadata("T123", "C123", "tech-news"))
        );
        view.putObject("state")
                .putObject("values")
                .putObject("company_subscriptions")
                .putObject("selected_companies")
                .putArray("selected_options")
                .addObject()
                .put("value", "netflix");
        return root.toString();
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(("v0:" + timestamp + ":" + body).getBytes(StandardCharsets.UTF_8));
        return "v0=" + HexFormat.of().formatHex(digest);
    }

    private record TestMetadata(String teamId, String channelId, String channelName) {
    }
}
