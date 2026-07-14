package com.globaltechblogarchive.slack.support;

import com.globaltechblogarchive.slack.config.SlackProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SlackRequestSignatureVerifier {

    private static final String SIGNATURE_VERSION = "v0";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long MAX_REQUEST_AGE_SECONDS = 5 * 60;

    private final String signingSecret;
    private final Clock clock;

    @Autowired
    public SlackRequestSignatureVerifier(SlackProperties properties) {
        this(properties.signingSecret(), Clock.systemUTC());
    }

    SlackRequestSignatureVerifier(String signingSecret, Clock clock) {
        this.signingSecret = signingSecret;
        this.clock = clock;
    }

    public boolean verify(String timestamp, String signature, byte[] rawBody) {
        if (!StringUtils.hasText(signingSecret)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(signature)
                || rawBody == null) {
            return false;
        }

        long requestEpochSecond;
        try {
            requestEpochSecond = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return false;
        }

        long currentEpochSecond = clock.instant().getEpochSecond();
        if (requestEpochSecond < currentEpochSecond - MAX_REQUEST_AGE_SECONDS
                || requestEpochSecond > currentEpochSecond + MAX_REQUEST_AGE_SECONDS) {
            return false;
        }

        String expectedSignature = createSignature(timestamp, rawBody);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String createSignature(String timestamp, byte[] rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            mac.update((SIGNATURE_VERSION + ":" + timestamp + ":").getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_VERSION + "=" + HexFormat.of().formatHex(mac.doFinal(rawBody));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }
}
