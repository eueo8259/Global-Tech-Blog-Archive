package com.globaltechblogarchive.crawl.api.dto;

import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;

public record SourceCrawlSummaryResponse(
        String companyKey,
        String sourceKey,
        boolean success,
        int candidateCount,
        int duplicateCount,
        int storedCount,
        int aiApprovedCount,
        int aiRejectedCount,
        int aiFailedCount,
        int previouslyApprovedCount,
        int previouslyRejectedCount,
        String errorMessage
) {

    public static SourceCrawlSummaryResponse from(SourceCrawlResult result) {
        CrawlRunSummary summary = result.summary();
        return new SourceCrawlSummaryResponse(
                result.companyKey(),
                result.sourceKey(),
                result.success(),
                result.candidateCount(),
                summary.duplicateCount(),
                summary.storedCount(),
                summary.aiApprovedCount(),
                summary.aiRejectedCount(),
                summary.aiFailedCount(),
                summary.previouslyApprovedCount(),
                summary.previouslyRejectedCount(),
                result.errorMessage()
        );
    }
}
