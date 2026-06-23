package com.globaltechblogarchive.crawl.application.dto;

import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.List;

public record SourceCrawlResult(
        String companyKey,
        String companyName,
        String sourceKey,
        String sourceName,
        boolean success,
        String errorMessage,
        int candidateCount,
        List<ArticleCandidate> candidates,
        List<String> qualityWarnings,
        CrawlRunSummary summary
) {

    public static SourceCrawlResult success(
            BlogSource source,
            List<ArticleCandidate> candidates,
            CrawlRunSummary summary
    ) {
        return new SourceCrawlResult(
                source.getCompany().getCompanyKey(),
                source.getCompany().getCompanyName(),
                source.getSourceKey(),
                source.getSourceName(),
                true,
                null,
                candidates.size(),
                candidates,
                sourceWarnings(candidates),
                summary
        );
    }

    public static SourceCrawlResult failure(
            BlogSource source,
            String errorMessage
    ) {
        return new SourceCrawlResult(
                source.getCompany().getCompanyKey(),
                source.getCompany().getCompanyName(),
                source.getSourceKey(),
                source.getSourceName(),
                false,
                errorMessage,
                0,
                List.of(),
                List.of("SOURCE_FETCH_FAILED"),
                CrawlRunSummary.empty()
        );
    }

    private static List<String> sourceWarnings(List<ArticleCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of("NO_CANDIDATES_DISCOVERED");
        }
        boolean allCandidatesNeedReview = candidates.stream()
                .allMatch(candidate -> !candidate.validationWarnings().isEmpty());
        if (allCandidatesNeedReview) {
            return List.of("ALL_CANDIDATES_NEED_REVIEW");
        }
        return List.of();
    }
}
