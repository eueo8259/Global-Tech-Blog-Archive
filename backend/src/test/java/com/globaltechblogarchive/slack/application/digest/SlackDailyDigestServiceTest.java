package com.globaltechblogarchive.slack.application.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SlackDailyDigestServiceTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 8, 3);
    private static final LocalDateTime NOW = DELIVERY_DATE.atTime(9, 0);

    private final SlackDailyDigestRunStateService runStateService = mock(SlackDailyDigestRunStateService.class);
    private final SlackDeliveryStateService deliveryStateService = mock(SlackDeliveryStateService.class);
    private final SlackDeliveryPreparationService preparationService = mock(SlackDeliveryPreparationService.class);
    private final SlackDeliveryDispatchService dispatchService = mock(SlackDeliveryDispatchService.class);
    private final SlackDeliveryRepository deliveryRepository = mock(SlackDeliveryRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-03T00:01:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final SlackDailyDigestService service = new SlackDailyDigestService(
            runStateService,
            deliveryStateService,
            preparationService,
            dispatchService,
            deliveryRepository,
            clock
    );

    @Test
    void runDailyPreparesAndDispatchesReadyDeliveriesThenCompletesRun() {
        when(runStateService.start(DELIVERY_DATE, NOW, NOW)).thenReturn(Optional.of(10L));
        when(deliveryStateService.recoverStaleProcessing(NOW)).thenReturn(1);
        when(preparationService.prepare(DELIVERY_DATE, NOW)).thenReturn(2);
        when(deliveryRepository.findReadyDeliveryIds(
                SlackDeliveryStatus.PENDING,
                SlackDeliveryStatus.RETRY_WAITING,
                NOW
        )).thenReturn(List.of(100L, 101L));

        SlackDailyDigestRunResult result = service.runDaily(DELIVERY_DATE, NOW, NOW);

        assertThat(result).isEqualTo(new SlackDailyDigestRunResult(true, 1, 2, 2));
        verify(dispatchService).dispatch(100L, NOW);
        verify(dispatchService).dispatch(101L, NOW);
        verify(runStateService).complete(10L, NOW.plusMinutes(1), 1, 2, 2);
    }

    @Test
    void runDailySkipsWhenDateRunCannotStart() {
        when(runStateService.start(DELIVERY_DATE, NOW, NOW)).thenReturn(Optional.empty());

        SlackDailyDigestRunResult result = service.runDaily(DELIVERY_DATE, NOW, NOW);

        assertThat(result.started()).isFalse();
        verify(preparationService, never()).prepare(DELIVERY_DATE, NOW);
    }

    @Test
    void runDailyMarksRunFailedWhenOrchestrationFails() {
        RuntimeException failure = new RuntimeException("prepare failed");
        when(runStateService.start(DELIVERY_DATE, NOW, NOW)).thenReturn(Optional.of(10L));
        doThrow(failure).when(preparationService).prepare(DELIVERY_DATE, NOW);

        assertThatThrownBy(() -> service.runDaily(DELIVERY_DATE, NOW, NOW))
                .isSameAs(failure);
        verify(runStateService).fail(
                10L,
                NOW.plusMinutes(1),
                "DAILY_DIGEST_RUN_FAILED",
                "prepare failed"
        );
    }

    @Test
    void runRetryRecoversAndDispatchesWithoutDailyRunRecord() {
        when(deliveryStateService.recoverStaleProcessing(NOW)).thenReturn(1);
        when(deliveryRepository.findReadyDeliveryIds(
                SlackDeliveryStatus.PENDING,
                SlackDeliveryStatus.RETRY_WAITING,
                NOW
        )).thenReturn(List.of(100L));

        SlackDeliveryRetryResult result = service.runRetry(NOW);

        assertThat(result).isEqualTo(new SlackDeliveryRetryResult(1, 1));
        verify(dispatchService).dispatch(100L, NOW);
        verify(runStateService, never()).start(DELIVERY_DATE, NOW, NOW);
    }
}
