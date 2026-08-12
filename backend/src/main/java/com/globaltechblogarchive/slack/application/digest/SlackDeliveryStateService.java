package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.config.SlackDailyDigestProperties;
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlackDeliveryStateService {

    private final SlackDeliveryRepository deliveryRepository;
    private final SlackDailyDigestProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedSlackDelivery> claim(Long deliveryId, LocalDateTime now) {
        SlackDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElse(null);
        if (delivery == null || !delivery.canStart(now)) {
            return Optional.empty();
        }
        delivery.startProcessing(now);
        return Optional.of(new ClaimedSlackDelivery(
                delivery.getId(),
                delivery.getSlackChannel().getId(),
                delivery.getSlackChannel().getSlackChannelId(),
                delivery.getSlackChannel().getWorkspace().getEncryptedBotToken(),
                delivery.getDeliveryKey(),
                delivery.getWindowStartedAt(),
                delivery.getWindowEndedAt(),
                delivery.getAttemptCount()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean startSendAttempt(Long deliveryId, LocalDateTime startedAt) {
        SlackDelivery delivery = processingDelivery(deliveryId);
        return delivery.startSendAttempt(startedAt, properties.maxAttempts());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long deliveryId, LocalDateTime completedAt, String messageTs) {
        SlackDelivery delivery = processingOrVerifyingDelivery(deliveryId);
        delivery.markSent(completedAt, messageTs);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markVerifying(
            Long deliveryId,
            LocalDateTime verificationAt,
            String messageTs,
            String errorCode,
            String errorMessage
    ) {
        SlackDelivery delivery = processingDelivery(deliveryId);
        delivery.markVerifying(verificationAt, messageTs, errorCode, errorMessage);
    }

    @Transactional(readOnly = true)
    public Optional<VerifyingSlackDelivery> findVerificationTarget(
            Long deliveryId,
            LocalDateTime now
    ) {
        SlackDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null
                || delivery.getStatus() != SlackDeliveryStatus.VERIFYING
                || delivery.getNextVerificationAt() == null
                || delivery.getNextVerificationAt().isAfter(now)) {
            return Optional.empty();
        }
        return Optional.of(new VerifyingSlackDelivery(
                delivery.getId(),
                delivery.getSlackChannel().getSlackChannelId(),
                delivery.getSlackChannel().getWorkspace().getEncryptedBotToken(),
                delivery.getDeliveryKey(),
                delivery.getProcessingStartedAt(),
                delivery.getSlackMessageTs(),
                delivery.getHistoryCursor(),
                delivery.getHistoryLatestAt()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void continueVerification(
            Long deliveryId,
            LocalDateTime nextAt,
            String nextCursor,
            LocalDateTime latestAt
    ) {
        SlackDelivery delivery = verifyingDelivery(deliveryId);
        delivery.continueVerification(nextAt, nextCursor, latestAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVerificationNotFound(Long deliveryId, LocalDateTime nextAt) {
        SlackDelivery delivery = verifyingDelivery(deliveryId);
        delivery.recordVerificationNotFound(
                nextAt,
                properties.maxVerificationChecks(),
                properties.maxAttempts()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleNextVerification(
            Long deliveryId,
            LocalDateTime nextAt,
            String errorCode,
            String errorMessage
    ) {
        SlackDelivery delivery = verifyingDelivery(deliveryId);
        delivery.scheduleNextVerification(nextAt, errorCode, errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(
            Long deliveryId,
            LocalDateTime failedAt,
            boolean retryable,
            Duration retryAfter,
            String errorCode,
            String errorMessage
    ) {
        SlackDelivery delivery = processingDelivery(deliveryId);
        if (retryable && delivery.getAttemptCount() < properties.maxAttempts()) {
            Duration delay = retryAfter;
            if (delay == null) {
                delay = properties.retryDelay();
            }
            delivery.markRetryWaiting(failedAt.plus(delay), errorCode, errorMessage);
            return;
        }
        delivery.markFailed(errorCode, errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPreparationFailure(
            Long deliveryId,
            LocalDateTime failedAt,
            String errorCode,
            String errorMessage
    ) {
        SlackDelivery delivery = processingDelivery(deliveryId);
        delivery.recordPreparationFailure(
                failedAt.plus(properties.retryDelay()),
                properties.maxAttempts(),
                errorCode,
                errorMessage
        );
    }

    @Transactional
    public int recoverStaleProcessing(LocalDateTime now) {
        LocalDateTime threshold = now.minus(properties.staleTimeout());
        List<SlackDelivery> staleDeliveries = deliveryRepository.findByStatusAndProcessingStartedAtBefore(
                SlackDeliveryStatus.PROCESSING,
                threshold
        );
        staleDeliveries.forEach(delivery -> delivery.recoverStaleProcessing(now));
        return staleDeliveries.size();
    }

    private SlackDelivery processingDelivery(Long deliveryId) {
        SlackDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Slack Delivery를 찾을 수 없습니다: " + deliveryId));
        if (delivery.getStatus() != SlackDeliveryStatus.PROCESSING) {
            throw new IllegalStateException("Slack Delivery가 PROCESSING 상태가 아닙니다: " + deliveryId);
        }
        return delivery;
    }

    private SlackDelivery processingOrVerifyingDelivery(Long deliveryId) {
        SlackDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Slack Delivery를 찾을 수 없습니다: " + deliveryId));
        if (delivery.getStatus() != SlackDeliveryStatus.PROCESSING
                && delivery.getStatus() != SlackDeliveryStatus.VERIFYING) {
            throw new IllegalStateException(
                    "Slack Delivery가 PROCESSING 또는 VERIFYING 상태가 아닙니다: " + deliveryId
            );
        }
        return delivery;
    }

    private SlackDelivery verifyingDelivery(Long deliveryId) {
        SlackDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Slack Delivery를 찾을 수 없습니다: " + deliveryId));
        if (delivery.getStatus() != SlackDeliveryStatus.VERIFYING) {
            throw new IllegalStateException("Slack Delivery가 VERIFYING 상태가 아닙니다: " + deliveryId);
        }
        return delivery;
    }
}
