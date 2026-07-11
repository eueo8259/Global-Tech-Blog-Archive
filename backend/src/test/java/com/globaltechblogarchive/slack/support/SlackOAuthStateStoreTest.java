package com.globaltechblogarchive.slack.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class SlackOAuthStateStoreTest {

    private final SlackOAuthStateStore stateStore = new SlackOAuthStateStore();

    @Test
    void createStateStoresStateInSession() {
        MockHttpSession session = new MockHttpSession();

        String state = stateStore.createState(session);

        assertThat(state).isNotBlank();
        assertThat(session.getAttribute("SLACK_OAUTH_STATE")).isEqualTo(state);
    }

    @Test
    void validateAndConsumeReturnsTrueForSavedStateAndRemovesIt() {
        MockHttpSession session = new MockHttpSession();
        String state = stateStore.createState(session);

        boolean valid = stateStore.validateAndConsume(session, state);

        assertThat(valid).isTrue();
        assertThat(session.getAttribute("SLACK_OAUTH_STATE")).isNull();
    }

    @Test
    void validateAndConsumeReturnsFalseForInvalidState() {
        MockHttpSession session = new MockHttpSession();
        stateStore.createState(session);

        boolean valid = stateStore.validateAndConsume(session, "invalid-state");

        assertThat(valid).isFalse();
        assertThat(session.getAttribute("SLACK_OAUTH_STATE")).isNull();
    }
}
