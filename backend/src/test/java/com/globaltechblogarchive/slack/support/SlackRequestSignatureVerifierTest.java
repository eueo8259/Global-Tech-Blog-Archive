package com.globaltechblogarchive.slack.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SlackRequestSignatureVerifierTest {

    private static final String SIGNING_SECRET = "8f742231b10e8888abcd99yyyzzz85a5";
    private static final long CURRENT_EPOCH_SECOND = 1_531_420_618L;
    private static final String TIMESTAMP = Long.toString(CURRENT_EPOCH_SECOND);
    private static final String RAW_BODY = "token=xyzz0WbapA4vBCDEFasx0q6G&team_id=T1DC2JH3J&team_domain=testteamnow"
            + "&channel_id=G8PSS9T3V&channel_name=foobar&user_id=U2CERLKJA&user_name=roadrunner"
            + "&command=%2Fwebhook-collect&text=&response_url=https%3A%2F%2Fhooks.slack.com%2Fcommands"
            + "%2FT1DC2JH3J%2F397700885554%2F96rGlfmibIGlgcZRskXaIFfN&trigger_id="
            + "398738663015.47445629121.803a0bc887a14d10d2c447fce8b6703c";
    private static final String SIGNATURE = "v0=a2114d57b48eac39b9ad189dd8316235a7b4a8d21a10bd27519666489c69b503";

    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(CURRENT_EPOCH_SECOND), ZoneOffset.UTC);
    private final SlackRequestSignatureVerifier verifier = new SlackRequestSignatureVerifier(SIGNING_SECRET, clock);

    @Test
    void verifyAcceptsValidSlackSignature() {
        boolean valid = verifier.verify(TIMESTAMP, SIGNATURE, body(RAW_BODY));

        assertThat(valid).isTrue();
    }

    @Test
    void verifyRejectsSignatureWhenRawBodyChanges() {
        boolean valid = verifier.verify(TIMESTAMP, SIGNATURE, body(RAW_BODY + "changed"));

        assertThat(valid).isFalse();
    }

    @Test
    void verifyRejectsMissingOrMalformedHeaders() {
        assertThat(verifier.verify(null, SIGNATURE, body(RAW_BODY))).isFalse();
        assertThat(verifier.verify(TIMESTAMP, null, body(RAW_BODY))).isFalse();
        assertThat(verifier.verify("not-a-number", SIGNATURE, body(RAW_BODY))).isFalse();
    }

    @Test
    void verifyRejectsRequestsOlderThanFiveMinutes() {
        String timestamp = Long.toString(CURRENT_EPOCH_SECOND - 301);

        assertThat(verifier.verify(timestamp, SIGNATURE, body(RAW_BODY))).isFalse();
    }

    @Test
    void verifyRejectsRequestsMoreThanFiveMinutesInFuture() {
        String timestamp = Long.toString(CURRENT_EPOCH_SECOND + 301);

        assertThat(verifier.verify(timestamp, SIGNATURE, body(RAW_BODY))).isFalse();
    }

    @Test
    void verifyRejectsRequestsWhenSigningSecretIsMissing() {
        SlackRequestSignatureVerifier verifierWithoutSecret = new SlackRequestSignatureVerifier("", clock);

        assertThat(verifierWithoutSecret.verify(TIMESTAMP, SIGNATURE, body(RAW_BODY))).isFalse();
    }

    private byte[] body(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
