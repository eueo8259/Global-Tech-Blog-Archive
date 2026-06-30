package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleCrawlService {

    private static final String PROMPT_VERSION = "v1";
    private static final int MAX_AI_FAILURE_RETRY_LIMIT = 100;

    private final BlogSourceRepository blogSourceRepository;
    private final ArticleDiscoveryLogRepository collectionItemRepository;
    private final SourceCrawlProcessor sourceCrawlProcessor;
    private final CrawlTransactionService transactionService;

    public ArticleCrawlResult runScheduled() {
        return run(CrawlMode.RECENT);
    }

    public ArticleCrawlResult runSourceBackfill(String sourceKey) {
        return runSource(sourceKey, CrawlMode.BACKFILL);
    }

    public ArticleCrawlResult retryAiFailures(int limit) {
        validateRetryLimit(limit);
        List<ArticleDiscoveryLog> failures = collectionItemRepository.findUnresolvedAiFailures(
                ArticleCandidateDecisionStatus.AI_FAILED,
                PROMPT_VERSION,
                PageRequest.of(0, limit)
        );
        Long runId = transactionService.startRun();
        Map<Long, List<ArticleDiscoveryLog>> failuresBySource = failures.stream()
                .collect(Collectors.groupingBy(
                        failure -> failure.getSource().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<SourceCrawlResult> sourceResults = new ArrayList<>();
        CrawlRunSummary summary = CrawlRunSummary.empty();

        for (List<ArticleDiscoveryLog> sourceFailures : failuresBySource.values()) {
            BlogSource source = sourceFailures.getFirst().getSource();
            SourceCrawlResult sourceResult;
            try {
                sourceResult = sourceCrawlProcessor.retryAiFailures(
                        runId,
                        source.getId(),
                        sourceFailures.stream().map(ArticleDiscoveryLog::getId).toList()
                );
            } catch (RuntimeException exception) {
                sourceResult = SourceCrawlResult.failure(source, exception.getMessage());
            }
            sourceResults.add(sourceResult);
            summary = summary.plus(sourceResult.summary());
        }

        int successCount = (int) sourceResults.stream().filter(SourceCrawlResult::success).count();
        int failureCount = sourceResults.size() - successCount;
        transactionService.completeRun(runId, sourceResults.size(), successCount, failureCount, summary);
        return result(runId, sourceResults, summary, successCount, failureCount);
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
