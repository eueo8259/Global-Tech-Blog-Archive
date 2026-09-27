package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.domain.SlackDailyDigestRun;
import com.globaltechblogarchive.slack.repository.SlackDailyDigestRunRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SlackDailyDigestRunStateService {

    private final SlackDailyDigestRunRepository runRepository;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> start(
            LocalDate deliveryDate,
            LocalDateTime windowEndedAt,
            LocalDateTime startedAt
    ) {
        Optional<SlackDailyDigestRun> existing = runRepository.findByDeliveryDate(deliveryDate);
        if (existing.isEmpty()) {
            SlackDailyDigestRun run = SlackDailyDigestRun.start(deliveryDate, windowEndedAt, startedAt);
            return Optional.of(runRepository.saveAndFlush(run).getId());
        }

        SlackDailyDigestRun run = existing.get();
        if (!run.canRestart()) {
            return Optional.empty();
        }
        run.restart(windowEndedAt, startedAt);
        return Optional.of(run.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(
            Long runId,
            LocalDateTime completedAt,
            int recoveredCount,
            int createdCount,
            int readyCount
    ) {
        SlackDailyDigestRun run = findRun(runId);
        run.complete(completedAt, recoveredCount, createdCount, readyCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long runId, LocalDateTime failedAt, String errorCode, String errorMessage) {
        SlackDailyDigestRun run = findRun(runId);
        run.fail(failedAt, errorCode, errorMessage);
    }

    private SlackDailyDigestRun findRun(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Daily Digest 실행을 찾을 수 없습니다: " + runId));
    }
}
