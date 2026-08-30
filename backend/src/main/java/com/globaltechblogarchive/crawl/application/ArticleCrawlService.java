package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.PreparedSourceCrawl;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.config.CrawlExecutionProperties;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ArticleCrawlService {

    private final BlogSourceRepository blogSourceRepository;
    private final SourceCrawlProcessor sourceCrawlProcessor;
    private final CrawlPersistenceService persistenceService;
    private final CrawlTransactionService transactionService;
    private final ArticleAiReviewService aiReviewService;
    private final AiReviewProperties aiReviewProperties;
    private final Executor crawlSourceExecutor;
    private final Semaphore crawlSourcePersistenceLimiter;
    private final CrawlPipelineMetrics pipelineMetrics;
    private final CrawlExecutionProperties executionProperties;
    private final AtomicInteger activeSourcePreparations = new AtomicInteger();

    public ArticleCrawlService(
            BlogSourceRepository blogSourceRepository,
            SourceCrawlProcessor sourceCrawlProcessor,
            CrawlPersistenceService persistenceService,
            CrawlTransactionService transactionService,
            ArticleAiReviewService aiReviewService,
            AiReviewProperties aiReviewProperties,
            @Qualifier("crawlSourceExecutor") Executor crawlSourceExecutor,
            @Qualifier("crawlSourcePersistenceLimiter") Semaphore crawlSourcePersistenceLimiter,
            CrawlPipelineMetrics pipelineMetrics,
            CrawlExecutionProperties executionProperties
    ) {
        this.blogSourceRepository = blogSourceRepository;
        this.sourceCrawlProcessor = sourceCrawlProcessor;
        this.persistenceService = persistenceService;
        this.transactionService = transactionService;
        this.aiReviewService = aiReviewService;
        this.aiReviewProperties = aiReviewProperties;
        this.crawlSourceExecutor = crawlSourceExecutor;
        this.crawlSourcePersistenceLimiter = crawlSourcePersistenceLimiter;
        this.pipelineMetrics = pipelineMetrics;
        this.executionProperties = executionProperties;
    }

    public ArticleCrawlResult runScheduled() {
        return run(CrawlPolicy.recent());
    }

    public ArticleCrawlResult runSourceBackfill(String sourceKey, int maxCandidatesPerSource) {
        return runSource(sourceKey, backfillPolicy(maxCandidatesPerSource));
    }

    public ArticleCrawlResult runAllBackfill(int maxCandidatesPerSource) {
        return run(backfillPolicy(maxCandidatesPerSource));
    }

    public ArticleCrawlResult retryAiFailures(int limit) {
        validateRetryLimit(limit);
        AiReviewRunResult reviewResult = aiReviewService.retryFailed(limit);
        return new ArticleCrawlResult(
                null,
                0,
                0,
                0,
                reviewResult.candidateCount(),
                0,
                reviewResult.storedCount(),
                reviewResult.candidateCount(),
                reviewResult.approvedCount(),
                reviewResult.rejectedCount(),
                reviewResult.failedCount(),
                reviewResult.retryWaitingCount(),
                0,
                0,
                List.of()
        );
    }

    private ArticleCrawlResult run(CrawlPolicy policy) {
        return runSources(blogSourceRepository.findByEnabledTrue(), policy);
    }

    private ArticleCrawlResult runSource(String sourceKey, CrawlPolicy policy) {
        BlogSource source = blogSourceRepository.findBySourceKeyAndEnabledTrue(sourceKey)
                .orElseThrow(() -> new InvalidInputException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "Enabled source not found: " + sourceKey
                ));
        return runSources(List.of(source), policy);
    }

    private ArticleCrawlResult runSources(List<BlogSource> sources, CrawlPolicy policy) {
        long runStartedAt = System.nanoTime();
        Long runId = transactionService.startRun();
        long preparationStartedAt = System.nanoTime();
        List<CompletableFuture<SourcePreparation>> sourceTasks = sources.stream()
                .map(source -> {
                    long submittedAt = System.nanoTime();
                    return CompletableFuture.supplyAsync(
                            () -> prepareSource(runId, source, policy, submittedAt),
                            crawlSourceExecutor
                    );
                })
                .toList();
        List<SourcePreparation> preparedSources = sourceTasks.stream()
                .map(CompletableFuture::join)
                .toList();
        long preparationDurationNanos = System.nanoTime() - preparationStartedAt;

        logSourceMeasurements(runId, preparedSources);

        long persistenceStartedAt = System.nanoTime();
        List<SourceCrawlResult> sourceResults = preparedSources.stream()
                .map(prepared -> persistSource(runId, prepared))
                .toList();
        long persistenceDurationNanos = System.nanoTime() - persistenceStartedAt;
        CrawlRunSummary summary = sourceResults.stream()
                .map(SourceCrawlResult::summary)
                .reduce(CrawlRunSummary.empty(), CrawlRunSummary::plus);

        int successCount = (int) sourceResults.stream().filter(SourceCrawlResult::success).count();
        int failureCount = sourceResults.size() - successCount;

        transactionService.completeRun(
                runId,
                sources.size(),
                successCount,
                failureCount,
                summary
        );

        long totalDurationNanos = System.nanoTime() - runStartedAt;
        pipelineMetrics.recordRunDurations(
                java.time.Duration.ofNanos(totalDurationNanos),
                java.time.Duration.ofNanos(preparationDurationNanos),
                java.time.Duration.ofNanos(persistenceDurationNanos)
        );
        logRunMeasurement(
                runId,
                totalDurationNanos,
                preparationDurationNanos,
                persistenceDurationNanos,
                preparedSources,
                successCount,
                failureCount
        );

        return result(runId, sourceResults, summary, successCount, failureCount);
    }

    private SourcePreparation prepareSource(
            Long runId,
            BlogSource source,
            CrawlPolicy policy,
            long submittedAt
    ) {
        long startedAt = System.nanoTime();
        int activeSources = activeSourcePreparations.incrementAndGet();
        log.info(
                "crawl_source_task_measurement runId={} sourceKey={} event=start "
                        + "queueWaitMs={} activeSources={} thread={}",
                runId,
                source.getSourceKey(),
                millis(startedAt - submittedAt),
                activeSources,
                Thread.currentThread().getName()
        );
        try {
            PreparedSourceCrawl prepared = pipelineMetrics.recordCollection(
                    source.getSourceKey(),
                    () -> sourceCrawlProcessor.prepare(source.getId(), policy)
            );
            return SourcePreparation.success(
                    source,
                    prepared,
                    System.nanoTime() - startedAt
            );
        } catch (RuntimeException exception) {
            return SourcePreparation.failure(
                    source,
                    exception.getMessage(),
                    System.nanoTime() - startedAt
            );
        } finally {
            int activeSourcesAfter = activeSourcePreparations.decrementAndGet();
            log.info(
                    "crawl_source_task_measurement runId={} sourceKey={} event=finish "
                            + "executionMs={} activeSourcesAfter={} thread={}",
                    runId,
                    source.getSourceKey(),
                    millis(System.nanoTime() - startedAt),
                    activeSourcesAfter,
                    Thread.currentThread().getName()
            );
        }
    }

    private SourceCrawlResult persistSource(Long runId, SourcePreparation preparation) {
        crawlSourcePersistenceLimiter.acquireUninterruptibly();
        try {
            return pipelineMetrics.recordPersistence(
                    preparation.source().getSourceKey(),
                    () -> persistSourceInWriter(runId, preparation)
            );
        } finally {
            crawlSourcePersistenceLimiter.release();
        }
    }

    private SourceCrawlResult persistSourceInWriter(
            Long runId,
            SourcePreparation preparation
    ) {
        if (!preparation.success()) {
            transactionService.markSourceFailed(
                    preparation.source().getId(),
                    preparation.errorMessage()
            );
            return SourceCrawlResult.failure(
                    preparation.source(),
                    preparation.errorMessage()
            );
        }
        try {
            PreparedSourceCrawl prepared = preparation.prepared();
            return persistenceService.persistDiscoveredCandidates(
                    runId,
                    prepared.sourceId(),
                    prepared.candidates(),
                    prepared.decisionsByHash()
            );
        } catch (RuntimeException exception) {
            transactionService.markSourceFailed(
                    preparation.source().getId(),
                    exception.getMessage()
            );
            return SourceCrawlResult.failure(
                    preparation.source(),
                    exception.getMessage()
            );
        }
    }

    private ArticleCrawlResult result(
            Long runId,
            List<SourceCrawlResult> sourceResults,
            CrawlRunSummary summary,
            int successCount,
            int failureCount
    ) {
        return new ArticleCrawlResult(
                runId,
                sourceResults.size(),
                successCount,
                failureCount,
                summary.discoveredCount(),
                summary.duplicateCount(),
                summary.storedCount(),
                summary.candidateCount(),
                summary.aiApprovedCount(),
                summary.aiRejectedCount(),
                summary.aiFailedCount(),
                0,
                summary.previouslyApprovedCount(),
                summary.previouslyRejectedCount(),
                sourceResults
        );
    }

    private void logSourceMeasurements(
            Long runId,
            List<SourcePreparation> preparedSources
    ) {
        for (SourcePreparation preparation : preparedSources) {
            log.info(
                    "crawl_source_measurement runId={} sourceKey={} preparationMs={} success={}",
                    runId,
                    preparation.source().getSourceKey(),
                    millis(preparation.durationNanos()),
                    preparation.success()
            );
        }
    }

    private void logRunMeasurement(
            Long runId,
            long totalDurationNanos,
            long preparationDurationNanos,
            long persistenceDurationNanos,
            List<SourcePreparation> preparedSources,
            int successCount,
            int failureCount
    ) {
        SourcePreparation slowest = preparedSources.stream()
                .max(Comparator.comparingLong(SourcePreparation::durationNanos))
                .orElse(null);
        String slowestSourceKey = "none";
        long slowestSourceDurationNanos = 0;
        if (slowest != null) {
            slowestSourceKey = slowest.source().getSourceKey();
            slowestSourceDurationNanos = slowest.durationNanos();
        }

        log.info(
                "crawl_run_measurement runId={} concurrency={} totalMs={} preparationMs={} "
                        + "persistenceMs={} slowestSource={} slowestSourceMs={} "
                        + "successCount={} failureCount={}",
                runId,
                executionProperties.sourceConcurrency(),
                millis(totalDurationNanos),
                millis(preparationDurationNanos),
                millis(persistenceDurationNanos),
                slowestSourceKey,
                millis(slowestSourceDurationNanos),
                successCount,
                failureCount
        );
    }

    private long millis(long durationNanos) {
        return TimeUnit.NANOSECONDS.toMillis(durationNanos);
    }

    private void validateRetryLimit(int limit) {
        if (limit < 1 || limit > aiReviewProperties.maxFailureRetryLimit()) {
            throw new InvalidInputException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "AI failure retry limit must be between 1 and "
                            + aiReviewProperties.maxFailureRetryLimit()
            );
        }
    }

    private CrawlPolicy backfillPolicy(int maxCandidatesPerSource) {
        if (maxCandidatesPerSource < 1
                || maxCandidatesPerSource > CrawlPolicy.BACKFILL_MAX_CANDIDATES) {
            throw new InvalidInputException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Backfill max candidates per source must be between 1 and "
                            + CrawlPolicy.BACKFILL_MAX_CANDIDATES
            );
        }
        return CrawlPolicy.backfill(maxCandidatesPerSource);
    }

    private record SourcePreparation(
            BlogSource source,
            PreparedSourceCrawl prepared,
            String errorMessage,
            long durationNanos
    ) {

        private static SourcePreparation success(
                BlogSource source,
                PreparedSourceCrawl prepared,
                long durationNanos
        ) {
            return new SourcePreparation(source, prepared, null, durationNanos);
        }

        private static SourcePreparation failure(
                BlogSource source,
                String errorMessage,
                long durationNanos
        ) {
            return new SourcePreparation(source, null, errorMessage, durationNanos);
        }

        private boolean success() {
            return prepared != null;
        }
    }
}
