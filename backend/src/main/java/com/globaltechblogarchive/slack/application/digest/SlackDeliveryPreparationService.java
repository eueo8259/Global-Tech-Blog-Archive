package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.repository.SlackChannelRepository;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlackDeliveryPreparationService {

    private static final EnumSet<SlackDeliveryStatus> ACTIVE_STATUSES = EnumSet.of(
            SlackDeliveryStatus.PENDING,
            SlackDeliveryStatus.PROCESSING,
            SlackDeliveryStatus.VERIFYING,
            SlackDeliveryStatus.RETRY_WAITING
    );
    private static final EnumSet<SlackDeliveryStatus> DELIVERED_STATUSES = EnumSet.of(
            SlackDeliveryStatus.SENT
    );

    private final SlackChannelRepository channelRepository;
    private final SlackDeliveryRepository deliveryRepository;
    private final SlackDigestQueryService digestQueryService;

    @Transactional
    public int prepare(LocalDate deliveryDate, LocalDateTime windowEndedAt) {
        List<SlackChannel> channels = channelRepository.findAllSubscribedChannelsWithWorkspace();
        int createdCount = 0;
        for (SlackChannel channel : channels) {
            if (deliveryRepository.existsBySlackChannelAndDeliveryDate(
                    channel,
                    deliveryDate
            )) {
                continue;
            }
            if (deliveryRepository.existsBySlackChannelAndStatusIn(channel, ACTIVE_STATUSES)) {
                continue;
            }

            LocalDateTime windowStartedAt = previousWindowEnd(channel);
            if (!windowEndedAt.isAfter(windowStartedAt)) {
                continue;
            }
            if (!digestQueryService.hasArticles(channel.getId(), windowStartedAt, windowEndedAt)) {
                continue;
            }

            deliveryRepository.save(SlackDelivery.pending(
                    channel,
                    deliveryDate,
                    windowStartedAt,
                    windowEndedAt
            ));
            createdCount++;
        }
        return createdCount;
    }

    private LocalDateTime previousWindowEnd(SlackChannel channel) {
        return deliveryRepository.findTopBySlackChannelAndStatusInOrderByWindowEndedAtDesc(
                        channel,
                        DELIVERED_STATUSES
                )
                .map(SlackDelivery::getWindowEndedAt)
                .orElse(channel.getCreatedAt());
    }
}
