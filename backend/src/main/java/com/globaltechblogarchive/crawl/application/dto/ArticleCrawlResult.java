package com.globaltechblogarchive.crawl.application.dto;

import java.util.List;

public record ArticleCrawlResult(
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
        List<SourceCrawlResult> sources
) {
}
