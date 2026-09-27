package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.application.SlackMessageClient;
import com.globaltechblogarchive.slack.application.SlackMessageSendResult;
import com.globaltechblogarchive.slack.application.SlackSendCertainty;
import com.globaltechblogarchive.slack.exception.SlackMessageSendException;
import com.globaltechblogarchive.slack.exception.SlackTokenDecryptionException;
import com.globaltechblogarchive.slack.support.SlackTokenEncryptor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackDeliveryDispatchService {

    private final SlackDeliveryStateService stateService;
    private final SlackDigestQueryService digestQueryService;
    private final SlackDigestMessageFactory messageFactory;
    private final SlackMessageClient messageClient;
    private final SlackTokenEncryptor tokenEncryptor;

    public void dispatch(Long deliveryId, LocalDateTime now) {
        Optional<ClaimedSlackDelivery> claimed = stateService.claim(deliveryId, now);
        if (claimed.isEmpty()) {
            return;
        }

        ClaimedSlackDelivery delivery = claimed.get();
        SlackChatMessage message;
        String botToken;
        try {
            List<SlackDigestArticle> articles = digestQueryService.findArticles(
                    delivery.channelId(),
                    delivery.windowStartedAt(),
                    delivery.windowEndedAt()
            );
            if (articles.isEmpty()) {
                stateService.markSent(delivery.deliveryId(), now, null);
                return;
            }

            message = messageFactory.create(
                    delivery.slackChannelId(),
                    delivery.deliveryKey(),
                    articles
            );
            botToken = tokenEncryptor.decrypt(delivery.encryptedBotToken());
        } catch (SlackTokenDecryptionException exception) {
            stateService.markFailure(
                    delivery.deliveryId(),
                    now,
                    false,
                    null,
                    "TOKEN_DECRYPTION_ERROR",
                    exception.getMessage()
            );
            log.warn(
                    "Slack Daily Digest 토큰 복호화 실패: deliveryId={}, channelId={}",
                    delivery.deliveryId(),
                    delivery.slackChannelId(),
                    exception
            );
            return;
        } catch (RuntimeException exception) {
            stateService.recordPreparationFailure(
                    delivery.deliveryId(),
                    now,
                    "UNEXPECTED_ERROR",
                    exception.getMessage()
            );
            log.warn(
                    "Slack Daily Digest 처리 중 예상하지 못한 실패: deliveryId={}, channelId={}",
                    delivery.deliveryId(),
                    delivery.slackChannelId(),
                    exception
            );
            return;
        }

        SlackMessageSendResult result;
        try {
            if (!stateService.startSendAttempt(delivery.deliveryId(), now)) {
                log.warn(
                        "Slack Daily Digest 최대 발송 시도 횟수 도달: deliveryId={}, channelId={}",
                        delivery.deliveryId(),
                        delivery.slackChannelId()
                );
                return;
            }
            result = messageClient.send(botToken, message);
        } catch (SlackMessageSendException exception) {
            if (exception.getCertainty() == SlackSendCertainty.UNKNOWN) {
                markVerifying(
                        delivery,
                        now,
                        null,
                        exception.getErrorCode(),
                        exception.getMessage()
                );
            } else {
                stateService.markFailure(
                        delivery.deliveryId(),
                        now,
                        exception.isRetryable(),
                        exception.getRetryAfter(),
                        exception.getErrorCode(),
                        exception.getMessage()
                );
            }
            log.warn(
                    "Slack Daily Digest 발송 실패: deliveryId={}, channelId={}, errorCode={}, certainty={}",
                    delivery.deliveryId(),
                    delivery.slackChannelId(),
                    exception.getErrorCode(),
                    exception.getCertainty()
            );
            return;
        }

        try {
            stateService.markSent(delivery.deliveryId(), now, result.messageTs());
        } catch (RuntimeException sentStatusFailure) {
            try {
                stateService.markVerifying(
                        delivery.deliveryId(),
                        now,
                        result.messageTs(),
                        "SENT_STATUS_SAVE_FAILED",
                        sentStatusFailure.getMessage()
                );
            } catch (RuntimeException fallbackStatusFailure) {
                sentStatusFailure.addSuppressed(fallbackStatusFailure);
                log.error(
                        "Slack message was sent but neither SENT nor VERIFYING status could be saved: "
                                + "deliveryId={}, channelId={}",
                        delivery.deliveryId(),
                        delivery.slackChannelId(),
                        sentStatusFailure
                );
                return;
            }
            log.error(
                    "Slack message was sent but its SENT status could not be saved; verification scheduled: "
                            + "deliveryId={}, channelId={}",
                    delivery.deliveryId(),
                    delivery.slackChannelId(),
                    sentStatusFailure
            );
        }
    }

    private void markVerifying(
            ClaimedSlackDelivery delivery,
            LocalDateTime now,
            String messageTs,
            String errorCode,
            String errorMessage
    ) {
        try {
            stateService.markVerifying(
                    delivery.deliveryId(),
                    now,
                    messageTs,
                    errorCode,
                    errorMessage
            );
        } catch (RuntimeException stateFailure) {
            log.error(
                    "Slack 발송 결과가 불확실하지만 VERIFYING 상태를 저장하지 못했습니다: "
                            + "deliveryId={}, channelId={}",
                    delivery.deliveryId(),
                    delivery.slackChannelId(),
                    stateFailure
            );
        }
    }
}
