package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.domain.SlackDeliveryStatus;
import com.globaltechblogarchive.slack.repository.SlackDeliveryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${slack.bot-token-encryption.key-base64:}')")
public class SlackDailyDigestService {

    private final SlackDailyDigestRunStateService runStateService;
    private final SlackDeliveryStateService deliveryStateService;
    private final SlackDeliveryPreparationService preparationService;
    private final SlackDeliveryDispatchService dispatchService;
    private final SlackDeliveryRepository deliveryRepository;
    private final Clock clock;

    public SlackDailyDigestService(
            SlackDailyDigestRunStateService runStateService,
            SlackDeliveryStateService deliveryStateService,
            SlackDeliveryPreparationService preparationService,
            SlackDeliveryDispatchService dispatchService,
            SlackDeliveryRepository deliveryRepository,
            @Qualifier("slackDailyDigestClock") Clock clock
    ) {
        this.runStateService = runStateService;
        this.deliveryStateService = deliveryStateService;
        this.preparationService = preparationService;
        this.dispatchService = dispatchService;
        this.deliveryRepository = deliveryRepository;
        this.clock = clock;
    }

    public SlackDailyDigestRunResult runDaily(
            LocalDate deliveryDate,
            LocalDateTime windowEndedAt,
            LocalDateTime executionNow
    ) {
        Optional<Long> runId = runStateService.start(deliveryDate, windowEndedAt, executionNow);
        if (runId.isEmpty()) {
            return SlackDailyDigestRunResult.skipped();
        }

        int recoveredCount = 0;
        int createdCount = 0;
        int readyCount = 0;
        try {
            recoveredCount = deliveryStateService.recoverStaleProcessing(executionNow);
            createdCount = preparationService.prepare(deliveryDate, windowEndedAt);
            List<Long> deliveryIds = readyDeliveryIds(executionNow);
            readyCount = deliveryIds.size();
            dispatch(deliveryIds, executionNow);
            runStateService.complete(
                    runId.get(),
                    LocalDateTime.now(clock),
                    recoveredCount,
                    createdCount,
                    readyCount
            );
            return new SlackDailyDigestRunResult(true, recoveredCount, createdCount, readyCount);
        } catch (RuntimeException exception) {
            markRunFailed(runId.get(), exception);
            throw exception;
        }
    }

    public SlackDeliveryRetryResult runRetry(LocalDateTime executionNow) {
        int recoveredCount = deliveryStateService.recoverStaleProcessing(executionNow);
        List<Long> deliveryIds = readyDeliveryIds(executionNow);
        dispatch(deliveryIds, executionNow);
        return new SlackDeliveryRetryResult(recoveredCount, deliveryIds.size());
    }

    private List<Long> readyDeliveryIds(LocalDateTime executionNow) {
        return deliveryRepository.findReadyDeliveryIds(
                SlackDeliveryStatus.PENDING,
                SlackDeliveryStatus.RETRY_WAITING,
                executionNow
        );
    }

    private void dispatch(List<Long> deliveryIds, LocalDateTime executionNow) {
        for (Long deliveryId : deliveryIds) {
            try {
                dispatchService.dispatch(deliveryId, executionNow);
            } catch (RuntimeException exception) {
                log.error(
                        "Slack Daily Digest delivery processing failed: deliveryId={}",
                        deliveryId,
                        exception
                );
            }
        }
    }

    private void markRunFailed(Long runId, RuntimeException failure) {
        try {
            runStateService.fail(
                    runId,
                    LocalDateTime.now(clock),
                    "DAILY_DIGEST_RUN_FAILED",
                    failure.getMessage()
            );
        } catch (RuntimeException stateFailure) {
            failure.addSuppressed(stateFailure);
        }
    }
}
