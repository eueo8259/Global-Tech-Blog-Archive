package com.globaltechblogarchive.slack.support;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.slack.exception.SlackTokenDecryptionException;

public class SlackTokenEncryptor {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public SlackTokenEncryptor(String keyBase64) {
        this(keyBase64, new SecureRandom());
    }

    SlackTokenEncryptor(String keyBase64, SecureRandom secureRandom) {
        byte[] key = Base64.getDecoder().decode(keyBase64);
        if (key.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("Slack bot token encryption key must be 32 bytes");
        }
        this.secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
        this.secureRandom = secureRandom;
    }

    public String encrypt(String plainToken) {
        try {
            byte[] iv = randomIv();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] cipherTextWithTag = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedPayload = ByteBuffer.allocate(iv.length + cipherTextWithTag.length)
                    .put(iv)
                    .put(cipherTextWithTag)
                    .array();
            return Base64.getEncoder().encodeToString(encryptedPayload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt Slack token", exception);
        }
    }

    public String decrypt(String encryptedToken) {
        try {
            byte[] encryptedPayload = Base64.getDecoder().decode(encryptedToken);
            if (encryptedPayload.length <= IV_LENGTH_BYTES) {
                throw new SlackTokenDecryptionException(
                        ErrorCode.SLACK_TOKEN_DECRYPTION_ERROR,
                        "Encrypted Slack token payload is too short"
                );
            }

            byte[] iv = Arrays.copyOfRange(encryptedPayload, 0, IV_LENGTH_BYTES);
            byte[] cipherTextWithTag = Arrays.copyOfRange(encryptedPayload, IV_LENGTH_BYTES, encryptedPayload.length);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plainText = cipher.doFinal(cipherTextWithTag);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (SlackTokenDecryptionException exception) {
            throw exception;
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new SlackTokenDecryptionException(ErrorCode.SLACK_TOKEN_DECRYPTION_ERROR);
        }
    }

    private byte[] randomIv() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }
}
