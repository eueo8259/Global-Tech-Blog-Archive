package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ArticleCrawlService {

    private final BlogSourceRepository blogSourceRepository;
    private final SourceCrawlProcessor sourceCrawlProcessor;
    private final CrawlTransactionService transactionService;
    private final ArticleAiReviewService aiReviewService;
    private final AiReviewProperties aiReviewProperties;
    private final Executor crawlSourceExecutor;

    public ArticleCrawlService(
            BlogSourceRepository blogSourceRepository,
            SourceCrawlProcessor sourceCrawlProcessor,
            CrawlTransactionService transactionService,
            ArticleAiReviewService aiReviewService,
            AiReviewProperties aiReviewProperties,
            @Qualifier("crawlSourceExecutor") Executor crawlSourceExecutor
    ) {
        this.blogSourceRepository = blogSourceRepository;
        this.sourceCrawlProcessor = sourceCrawlProcessor;
        this.transactionService = transactionService;
        this.aiReviewService = aiReviewService;
        this.aiReviewProperties = aiReviewProperties;
        this.crawlSourceExecutor = crawlSourceExecutor;
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
        Long runId = transactionService.startRun();
        List<CompletableFuture<SourceCrawlResult>> sourceTasks = sources.stream()
                .map(source -> CompletableFuture.supplyAsync(
                        () -> processSource(runId, source, policy),
                        crawlSourceExecutor
                ))
                .toList();
        List<SourceCrawlResult> sourceResults = sourceTasks.stream()
                .map(CompletableFuture::join)
                .toList();
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

        return result(runId, sourceResults, summary, successCount, failureCount);
    }

    private SourceCrawlResult processSource(
            Long runId,
            BlogSource source,
            CrawlPolicy policy
    ) {
        try {
            return sourceCrawlProcessor.process(runId, source.getId(), policy);
        } catch (RuntimeException exception) {
            transactionService.markSourceFailed(source.getId(), exception.getMessage());
            return SourceCrawlResult.failure(source, exception.getMessage());
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
}
