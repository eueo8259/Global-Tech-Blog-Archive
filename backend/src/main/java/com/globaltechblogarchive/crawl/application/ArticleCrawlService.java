package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
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

    public ArticleCrawlResult run() {
        return run(CrawlMode.RECENT);
    }

    public ArticleCrawlResult runInitial() {
        return run(CrawlMode.INITIAL);
    }

    private ArticleCrawlResult run(CrawlMode mode) {
        Long runId = transactionService.startRun();
        List<BlogSource> sources = blogSourceRepository.findByEnabledTrue();
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
