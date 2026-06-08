package com.globaltechblogarchive.crawl.api.dto;

import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import java.util.List;

public record SourceCrawlResponse(
        String companyKey,
        String companyName,
        boolean success,
        String errorMessage,
        int candidateCount,
        List<ArticleCandidateResponse> candidates,
        List<String> qualityWarnings
) {

    public static SourceCrawlResponse from(SourceCrawlResult result) {
        return new SourceCrawlResponse(
                result.companyKey(),
                result.companyName(),
                result.success(),
                result.errorMessage(),
                result.candidateCount(),
                result.candidates().stream()
                        .map(ArticleCandidateResponse::from)
                        .toList(),
                result.qualityWarnings()
        );
    }
}
