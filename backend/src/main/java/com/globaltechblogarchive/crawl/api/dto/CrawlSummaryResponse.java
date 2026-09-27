package com.globaltechblogarchive.crawl.api.dto;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import java.util.List;

public record CrawlSummaryResponse(
        Long runId,
        int sourceCount,
        int successCount,
        int failureCount,
        int candidateCount,
        int duplicateCount,
        int storedCount,
        int aiApprovedCount,
        int aiRejectedCount,
        int aiFailedCount,
        int aiRetryWaitingCount,
        int previouslyApprovedCount,
        int previouslyRejectedCount,
        List<SourceCrawlSummaryResponse> sources
) {

    public static CrawlSummaryResponse from(ArticleCrawlResult result) {
        return new CrawlSummaryResponse(
                result.runId(),
                result.sourceCount(),
                result.successCount(),
                result.failureCount(),
                result.candidateCount(),
                result.duplicateCount(),
                result.storedCount(),
                result.aiApprovedCount(),
                result.aiRejectedCount(),
                result.aiFailedCount(),
                result.aiRetryWaitingCount(),
                result.previouslyApprovedCount(),
                result.previouslyRejectedCount(),
                result.sources().stream()
                        .map(SourceCrawlSummaryResponse::from)
                        .toList()
        );
    }
}
