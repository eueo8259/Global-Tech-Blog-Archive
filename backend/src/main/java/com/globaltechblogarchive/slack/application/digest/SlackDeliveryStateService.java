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
                delivery.getWindowStartedAt(),
                delivery.getWindowEndedAt(),
                delivery.getAttemptCount()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long deliveryId, LocalDateTime completedAt, String messageTs) {
        SlackDelivery delivery = processingDelivery(deliveryId);
        delivery.markSent(completedAt, messageTs);
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
}
