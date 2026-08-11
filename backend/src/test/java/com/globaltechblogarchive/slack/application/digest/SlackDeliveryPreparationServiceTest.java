package com.globaltechblogarchive.slack.application.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackDelivery;
import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.repository.SlackChannelRepository;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SlackDeliveryPreparationServiceTest {

    private final SlackChannelRepository channelRepository = mock(SlackChannelRepository.class);
    private final SlackDeliveryRepository deliveryRepository = mock(SlackDeliveryRepository.class);
    private final SlackDigestQueryService queryService = mock(SlackDigestQueryService.class);
    private final SlackDeliveryPreparationService service = new SlackDeliveryPreparationService(
            channelRepository,
            deliveryRepository,
            queryService
    );

    private SlackChannel channel;
    private LocalDate deliveryDate;
    private LocalDateTime windowStartedAt;
    private LocalDateTime windowEndedAt;

    @BeforeEach
    void setUp() {
        channel = mock(SlackChannel.class);
        deliveryDate = LocalDate.of(2026, 7, 14);
        windowStartedAt = LocalDateTime.of(2026, 7, 13, 9, 0);
        windowEndedAt = LocalDateTime.of(2026, 7, 14, 9, 0);
        when(channel.getId()).thenReturn(10L);
        when(channel.getCreatedAt()).thenReturn(windowStartedAt);
        when(channelRepository.findAllSubscribedChannelsWithWorkspace()).thenReturn(List.of(channel));
    }

    @Test
    void prepareCreatesDeliveryOnlyWhenChannelHasDigestArticles() {
        when(queryService.hasArticles(10L, windowStartedAt, windowEndedAt)).thenReturn(true);

        int createdCount = service.prepare(deliveryDate, windowEndedAt);

        assertThat(createdCount).isEqualTo(1);
        ArgumentCaptor<SlackDelivery> captor = ArgumentCaptor.forClass(SlackDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getWindowStartedAt()).isEqualTo(windowStartedAt);
        assertThat(captor.getValue().getWindowEndedAt()).isEqualTo(windowEndedAt);
    }

    @Test
    void prepareDoesNotCreateDeliveryForEmptyChannel() {
        when(queryService.hasArticles(10L, windowStartedAt, windowEndedAt)).thenReturn(false);

        int createdCount = service.prepare(deliveryDate, windowEndedAt);

        assertThat(createdCount).isZero();
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void prepareStartsAfterLatestConfirmedSendWindow() {
        LocalDateTime previousWindowEnd = windowStartedAt.plusHours(2);
        SlackDelivery previousDelivery = mock(SlackDelivery.class);
        when(previousDelivery.getWindowEndedAt()).thenReturn(previousWindowEnd);
        when(deliveryRepository.findTopBySlackChannelAndStatusInOrderByWindowEndedAtDesc(
                channel,
                EnumSet.of(SlackDeliveryStatus.SENT)
        )).thenReturn(Optional.of(previousDelivery));
        when(queryService.hasArticles(10L, previousWindowEnd, windowEndedAt)).thenReturn(true);

        int createdCount = service.prepare(deliveryDate, windowEndedAt);

        assertThat(createdCount).isEqualTo(1);
        ArgumentCaptor<SlackDelivery> captor = ArgumentCaptor.forClass(SlackDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertThat(captor.getValue().getWindowStartedAt()).isEqualTo(previousWindowEnd);
    }

    @Test
    void prepareDoesNotCreateDuplicateDeliveryForSameDate() {
        when(deliveryRepository.existsBySlackChannelAndDeliveryDate(
                channel,
                deliveryDate
        )).thenReturn(true);

        int createdCount = service.prepare(deliveryDate, windowEndedAt);

        assertThat(createdCount).isZero();
        verify(queryService, never()).hasArticles(any(), any(), any());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void prepareDoesNotCreateDeliveryWhilePreviousDeliveryIsVerifying() {
        when(deliveryRepository.existsBySlackChannelAndStatusIn(
                channel,
                EnumSet.of(
                        SlackDeliveryStatus.PENDING,
                        SlackDeliveryStatus.PROCESSING,
                        SlackDeliveryStatus.VERIFYING,
                        SlackDeliveryStatus.RETRY_WAITING
                )
        )).thenReturn(true);

        int createdCount = service.prepare(deliveryDate, windowEndedAt);

        assertThat(createdCount).isZero();
        verify(queryService, never()).hasArticles(any(), any(), any());
        verify(deliveryRepository, never()).save(any());
    }
}
