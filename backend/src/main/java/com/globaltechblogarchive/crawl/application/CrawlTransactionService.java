package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlTransactionService {

    private final ArticleCollectionRunRepository collectionRunRepository;
    private final BlogSourceRepository blogSourceRepository;

    @Transactional
    public Long startRun() {
        return collectionRunRepository.save(ArticleCollectionRun.start(LocalDateTime.now())).getId();
    }

    @Transactional
    public void completeRun(
            Long runId,
            int sourceCount,
            int successCount,
            int failureCount,
            CrawlRunSummary summary
    ) {
        ArticleCollectionRun run = collectionRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Collection run not found: " + runId));
        run.complete(
                LocalDateTime.now(),
                sourceCount,
                successCount,
                failureCount,
                summary.discoveredCount(),
                summary.duplicateCount(),
                summary.storedCount()
        );
    }

    @Transactional
    public void markSourceFailed(Long sourceId, String errorMessage) {
        BlogSource source = blogSourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Blog source not found: " + sourceId));
        source.markCollectionFailed(LocalDateTime.now(), errorMessage);
    }
}
