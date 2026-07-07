package com.globaltechblogarchive.slack.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.slack.exception.SlackTokenDecryptionException;

class SlackTokenEncryptorTest {

    private static final String KEY_BASE64 = base64Key("0123456789abcdef0123456789abcdef");
    private static final String OTHER_KEY_BASE64 = base64Key("abcdef0123456789abcdef0123456789");
    private static final String PLAIN_TOKEN = "xoxb-test-token";

    private final SlackTokenEncryptor encryptor = new SlackTokenEncryptor(KEY_BASE64);

    @Test
    void encryptReturnsDifferentValueFromPlainToken() {
        String encryptedToken = encryptor.encrypt(PLAIN_TOKEN);

        assertThat(encryptedToken).isNotEqualTo(PLAIN_TOKEN);
        assertThat(encryptedToken).doesNotContain(PLAIN_TOKEN);
    }

    @Test
    void encryptReturnsDifferentValuesForSamePlainToken() {
        String firstEncryptedToken = encryptor.encrypt(PLAIN_TOKEN);
        String secondEncryptedToken = encryptor.encrypt(PLAIN_TOKEN);

        assertThat(firstEncryptedToken).isNotEqualTo(secondEncryptedToken);
    }

    @Test
    void decryptReturnsOriginalTokenAfterEncryption() {
        String encryptedToken = encryptor.encrypt(PLAIN_TOKEN);

        String decryptedToken = encryptor.decrypt(encryptedToken);

        assertThat(decryptedToken).isEqualTo(PLAIN_TOKEN);
    }

    @Test
    void decryptThrowsExceptionWhenEncryptedTokenIsTampered() {
        String encryptedToken = encryptor.encrypt(PLAIN_TOKEN);
        String tamperedToken = tamperOneCharacter(encryptedToken);

        assertThatThrownBy(() -> encryptor.decrypt(tamperedToken))
                .isInstanceOf(SlackTokenDecryptionException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SLACK_TOKEN_DECRYPTION_ERROR);
    }

    @Test
    void decryptThrowsExceptionWhenKeyIsWrong() {
        String encryptedToken = encryptor.encrypt(PLAIN_TOKEN);
        SlackTokenEncryptor wrongKeyEncryptor = new SlackTokenEncryptor(OTHER_KEY_BASE64);

        assertThatThrownBy(() -> wrongKeyEncryptor.decrypt(encryptedToken))
                .isInstanceOf(SlackTokenDecryptionException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SLACK_TOKEN_DECRYPTION_ERROR);
    }

    private static String tamperOneCharacter(String encryptedToken) {
        char replacement = encryptedToken.charAt(0) == 'A' ? 'B' : 'A';
        return replacement + encryptedToken.substring(1);
    }

    private static String base64Key(String key) {
        return Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }
}
