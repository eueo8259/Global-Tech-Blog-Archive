package com.globaltechblogarchive.crawl.api.dto;

import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import java.util.List;

public record InitialArticleCrawlResponse(
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
        int previouslyApprovedCount,
        int previouslyRejectedCount,
        List<InitialSourceCrawlResponse> sources
) {

    public static InitialArticleCrawlResponse from(ArticleCrawlResult result) {
        return new InitialArticleCrawlResponse(
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
                result.previouslyApprovedCount(),
                result.previouslyRejectedCount(),
                result.sources().stream()
                        .map(InitialSourceCrawlResponse::from)
                        .toList()
        );
    }
}
