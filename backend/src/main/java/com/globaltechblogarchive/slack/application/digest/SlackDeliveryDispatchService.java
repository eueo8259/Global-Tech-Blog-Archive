package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.application.SlackMessageClient;
import com.globaltechblogarchive.slack.application.SlackMessageSendResult;
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

            SlackChatMessage message = messageFactory.create(delivery.slackChannelId(), articles);
            String botToken = tokenEncryptor.decrypt(delivery.encryptedBotToken());
            SlackMessageSendResult result = messageClient.send(botToken, message);
            stateService.markSent(delivery.deliveryId(), now, result.messageTs());
        } catch (SlackMessageSendException exception) {
            stateService.markFailure(
                    delivery.deliveryId(),
                    now,
                    exception.isRetryable(),
                    exception.getRetryAfter(),
                    exception.getErrorCode(),
                    exception.getMessage()
            );
            log.warn(
                    "Slack Daily Digest 발송 실패: deliveryId={}, channelId={}, errorCode={}, retryable={}",
                    delivery.deliveryId(),
                    delivery.slackChannelId(),
                    exception.getErrorCode(),
                    exception.isRetryable()
            );
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
        } catch (RuntimeException exception) {
            stateService.markFailure(
                    delivery.deliveryId(),
                    now,
                    true,
                    null,
                    "UNEXPECTED_ERROR",
                    exception.getMessage()
            );
            log.warn(
                    "Slack Daily Digest 처리 중 예상하지 못한 실패: deliveryId={}, channelId={}",
                    delivery.deliveryId(),
                    delivery.slackChannelId(),
                    exception
            );
        }
    }
}
