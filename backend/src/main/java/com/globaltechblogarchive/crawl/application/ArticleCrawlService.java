package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleCrawlService {

    private static final int MAX_AI_FAILURE_RETRY_LIMIT = 100;

    private final BlogSourceRepository blogSourceRepository;
    private final SourceCrawlProcessor sourceCrawlProcessor;
    private final CrawlTransactionService transactionService;
    private final ArticleAiReviewService aiReviewService;

    public ArticleCrawlResult runScheduled() {
        return run(CrawlMode.RECENT);
    }

    public ArticleCrawlResult runSourceBackfill(String sourceKey) {
        return runSource(sourceKey, CrawlMode.BACKFILL);
    }

    public ArticleCrawlResult retryAiFailures(int limit) {
        validateRetryLimit(limit);
        Long runId = transactionService.startRun();
        AiReviewRunResult reviewResult = aiReviewService.retryFailed(limit);
        CrawlRunSummary summary = new CrawlRunSummary(
                reviewResult.candidateCount(),
                0,
                reviewResult.storedCount(),
                reviewResult.approvedCount(),
                reviewResult.rejectedCount(),
                reviewResult.failedCount(),
                0,
                0
        );
        transactionService.completeRun(runId, 0, 0, 0, summary);
        return result(runId, List.of(), summary, 0, 0);
    }

    private ArticleCrawlResult run(CrawlMode mode) {
        return runSources(blogSourceRepository.findByEnabledTrue(), mode);
    }

    private ArticleCrawlResult runSource(String sourceKey, CrawlMode mode) {
        BlogSource source = blogSourceRepository.findBySourceKeyAndEnabledTrue(sourceKey)
                .orElseThrow(() -> new InvalidInputException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "Enabled source not found: " + sourceKey
                ));
        return runSources(List.of(source), mode);
    }

    private ArticleCrawlResult runSources(List<BlogSource> sources, CrawlMode mode) {
        Long runId = transactionService.startRun();
        List<SourceCrawlResult> sourceResults = new ArrayList<>();
        CrawlRunSummary summary = CrawlRunSummary.empty();

        for (BlogSource source : sources) {
            SourceCrawlResult sourceResult;
            try {
                sourceResult = sourceCrawlProcessor.process(runId, source.getId(), mode);
            } catch (RuntimeException exception) {
                transactionService.markSourceFailed(source.getId(), exception.getMessage());
                sourceResult = SourceCrawlResult.failure(source, exception.getMessage());
            }
            sourceResults.add(sourceResult);
            summary = summary.plus(sourceResult.summary());
        }

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
                summary.previouslyApprovedCount(),
                summary.previouslyRejectedCount(),
                sourceResults
        );
    }

    private void validateRetryLimit(int limit) {
        if (limit < 1 || limit > MAX_AI_FAILURE_RETRY_LIMIT) {
            throw new InvalidInputException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "AI failure retry limit must be between 1 and " + MAX_AI_FAILURE_RETRY_LIMIT
            );
        }
    }
}
