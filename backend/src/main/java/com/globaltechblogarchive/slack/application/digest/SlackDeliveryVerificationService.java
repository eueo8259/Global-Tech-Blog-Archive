package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.application.SlackMessageLookupClient;
import com.globaltechblogarchive.slack.application.SlackMessageLookupResult;
import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import com.globaltechblogarchive.slack.exception.SlackMessageLookupException;
import com.globaltechblogarchive.slack.exception.SlackTokenDecryptionException;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackDeliveryVerificationService {

    private static final Set<String> PERMISSION_ERROR_CODES = Set.of(
            "HTTP_401",
            "HTTP_403",
            "account_inactive",
            "access_denied",
            "channel_is_limited_access",
            "missing_scope",
            "no_permission",
            "not_allowed_token_type",
            "not_authed",
            "not_in_channel",
            "invalid_auth",
            "team_access_not_granted",
            "token_expired",
            "token_revoked"
    );

    private final SlackDeliveryStateService stateService;
    private final SlackMessageLookupClient messageLookupClient;
    private final SlackTokenEncryptor tokenEncryptor;
    private final SlackDailyDigestProperties properties;

    public void verify(Long deliveryId, LocalDateTime now) {
        Optional<VerifyingSlackDelivery> target = stateService.findVerificationTarget(
                deliveryId,
                now
        );
        if (target.isEmpty()) {
            return;
        }

        VerifyingSlackDelivery delivery = target.get();
        String botToken;
        try {
            botToken = tokenEncryptor.decrypt(delivery.encryptedBotToken());
        } catch (SlackTokenDecryptionException exception) {
            stateService.scheduleNextVerification(
                    deliveryId,
                    now.plus(properties.verificationPermissionErrorDelay()),
                    "TOKEN_DECRYPTION_ERROR",
                    exception.getMessage()
            );
            log.warn(
                    "Slack 발송 검증 토큰 복호화 실패: deliveryId={}, channelId={}",
                    deliveryId,
                    delivery.slackChannelId(),
                    exception
            );
            return;
        }

        try {
            Optional<SlackMessageLookupResult> result = messageLookupClient.findByDeliveryKey(
                    botToken,
                    delivery.slackChannelId(),
                    delivery.deliveryKey(),
                    delivery.attemptedAt(),
                    now,
                    delivery.knownMessageTs()
            );
            if (result.isPresent()) {
                stateService.markSent(deliveryId, now, result.get().messageTs());
                return;
            }
            stateService.recordVerificationNotFound(
                    deliveryId,
                    now.plus(properties.verificationDelay())
            );
        } catch (SlackMessageLookupException exception) {
            Duration delay = verificationFailureDelay(exception);
            stateService.scheduleNextVerification(
                    deliveryId,
                    now.plus(delay),
                    exception.getErrorCode(),
                    exception.getMessage()
            );
            log.warn(
                    "Slack 발송 검증 조회 실패: deliveryId={}, channelId={}, errorCode={}, retryable={}",
                    deliveryId,
                    delivery.slackChannelId(),
                    exception.getErrorCode(),
                    exception.isRetryable()
            );
        }
    }

    private Duration verificationFailureDelay(SlackMessageLookupException exception) {
        if (exception.getRetryAfter() != null) {
            return exception.getRetryAfter();
        }
        if (PERMISSION_ERROR_CODES.contains(exception.getErrorCode())) {
            return properties.verificationPermissionErrorDelay();
        }
        return properties.verificationDelay();
    }
}
