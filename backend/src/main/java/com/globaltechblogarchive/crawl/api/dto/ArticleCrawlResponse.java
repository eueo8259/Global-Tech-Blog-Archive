package com.globaltechblogarchive.crawl.api.dto;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
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
        int aiApprovedCount,
        int aiRejectedCount,
        int aiFailedCount,
        int previouslyApprovedCount,
        int previouslyRejectedCount,
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
                result.aiApprovedCount(),
                result.aiRejectedCount(),
                result.aiFailedCount(),
                result.previouslyApprovedCount(),
                result.previouslyRejectedCount(),
                result.sources().stream()
                        .map(SourceCrawlResponse::from)
                        .toList()
        );
    }
}
