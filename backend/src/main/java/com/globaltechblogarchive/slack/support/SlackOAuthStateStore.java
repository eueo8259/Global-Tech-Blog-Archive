package com.globaltechblogarchive.slack.support;

import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class SlackOAuthStateStore {

    private static final String SESSION_ATTRIBUTE = "SLACK_OAUTH_STATE";
    private static final int STATE_BYTES = 32;

    private final SecureRandom secureRandom;

    public SlackOAuthStateStore() {
        this(new SecureRandom());
    }

    SlackOAuthStateStore(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String createState(HttpSession session) {
        byte[] bytes = new byte[STATE_BYTES];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTRIBUTE, state);
        return state;
    }

    public boolean validateAndConsume(HttpSession session, String state) {
        Object savedState = session.getAttribute(SESSION_ATTRIBUTE);
        session.removeAttribute(SESSION_ATTRIBUTE);
        if (!(savedState instanceof String savedStateValue) || state == null) {
            return false;
        }
        return MessageDigest.isEqual(
                savedStateValue.getBytes(StandardCharsets.UTF_8),
                state.getBytes(StandardCharsets.UTF_8)
        );
    }
}
