package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleCrawlService {

    private static final String PROMPT_VERSION = "v1";

    private final BlogSourceRepository blogSourceRepository;
    private final ArticleCollectionRunRepository collectionRunRepository;
    private final SourceCrawlProcessor sourceCrawlProcessor;

    @Transactional
    public ArticleCrawlResult run() {
        ArticleCollectionRun run = collectionRunRepository.save(ArticleCollectionRun.start(LocalDateTime.now()));
        List<BlogSource> sources = blogSourceRepository.findByEnabledTrue();
        List<SourceCrawlResult> sourceResults = new ArrayList<>();
        CrawlRunSummary summary = CrawlRunSummary.empty();

        for (BlogSource source : sources) {
            SourceCrawlOutcome outcome = sourceCrawlProcessor.process(run, source, PROMPT_VERSION);
            sourceResults.add(outcome.result());
            summary = summary.plus(outcome.summary());
        }

        int successCount = (int) sourceResults.stream().filter(SourceCrawlResult::success).count();
        int failureCount = sourceResults.size() - successCount;
        run.complete(
                LocalDateTime.now(),
                sources.size(),
                successCount,
                failureCount,
                summary.discoveredCount(),
                summary.duplicateCount(),
                summary.storedCount()
        );
        return new ArticleCrawlResult(
                run.getId(),
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
