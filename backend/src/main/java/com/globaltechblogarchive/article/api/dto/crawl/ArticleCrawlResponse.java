package com.globaltechblogarchive.article.api.dto.crawl;

import com.globaltechblogarchive.collection.application.ArticleCrawlResult;
import java.util.List;

public record ArticleCrawlResponse(
        Long runId,
        int sourceCount,
        int successCount,
        int failureCount,
        int discoveredCount,
        int duplicateCount,
        int storedCount,
        int candidateCount,
        List<SourceCrawlResponse> sources
) {

    public static ArticleCrawlResponse from(ArticleCrawlResult result) {
        return new ArticleCrawlResponse(
                result.runId(),
                result.sourceCount(),
                result.successCount(),
                result.failureCount(),
                result.discoveredCount(),
                result.duplicateCount(),
                result.storedCount(),
                result.candidateCount(),
                result.sources().stream()
                        .map(SourceCrawlResponse::from)
                        .toList()
        );
    }
}
