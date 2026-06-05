package com.globaltechblogarchive.collection.application;

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
        List<SourceCrawlResult> sources
) {
}
