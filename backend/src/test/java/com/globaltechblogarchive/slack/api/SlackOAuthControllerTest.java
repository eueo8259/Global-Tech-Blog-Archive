package com.globaltechblogarchive.slack.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.global.error.GlobalExceptionHandler;
import com.globaltechblogarchive.slack.application.SlackOAuthService;
import com.globaltechblogarchive.slack.support.SlackOAuthStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SlackOAuthControllerTest {

    private SlackOAuthService slackOAuthService;
    private SlackOAuthStateStore stateStore;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        slackOAuthService = mock(SlackOAuthService.class);
        stateStore = mock(SlackOAuthStateStore.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SlackOAuthController(slackOAuthService, stateStore))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void authorizeRedirectsToSlackAuthorizeUrl() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(stateStore.createState(session)).thenReturn("state-123");
        when(slackOAuthService.createAuthorizeUrl("state-123"))
                .thenReturn("https://slack.com/oauth/v2/authorize?state=state-123");

        mockMvc.perform(get("/slack/oauth/authorize").session(session))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://slack.com/oauth/v2/authorize?state=state-123"));

        verify(stateStore).createState(session);
    }

    @Test
    void callbackInstallsWorkspaceWhenStateIsValid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(stateStore.validateAndConsume(session, "state-123")).thenReturn(true);

        mockMvc.perform(get("/slack/oauth/callback")
                        .session(session)
                        .param("code", "code-123")
                        .param("state", "state-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Slack installation completed."));

        verify(slackOAuthService).install("code-123");
    }

    @Test
    void callbackRejectsInvalidState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        when(stateStore.validateAndConsume(session, "bad-state")).thenReturn(false);

        mockMvc.perform(get("/slack/oauth/callback")
                        .session(session)
                        .param("code", "code-123")
                        .param("state", "bad-state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("Slack OAuth state is invalid"));

        verify(slackOAuthService, never()).install("code-123");
    }

    @Test
    void callbackRejectsSlackAuthorizationError() throws Exception {
        mockMvc.perform(get("/slack/oauth/callback")
                        .param("error", "access_denied"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("Slack OAuth authorization was cancelled"));

        verify(slackOAuthService, never()).install(org.mockito.ArgumentMatchers.anyString());
    }
}
