package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
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

    private final BlogSourceRepository blogSourceRepository;
    private final SourceCrawlProcessor sourceCrawlProcessor;
    private final CrawlTransactionService transactionService;

    public ArticleCrawlResult runScheduled() {
        return run(CrawlMode.RECENT);
    }

    public ArticleCrawlResult runSourceBackfill(String sourceKey) {
        return runSource(sourceKey, CrawlMode.BACKFILL);
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

        return new ArticleCrawlResult(
                runId,
                sources.size(),
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
}
