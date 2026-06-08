package com.globaltechblogarchive.crawl.application.dto;

import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.List;

public record SourceCrawlResult(
        String companyKey,
        String companyName,
        boolean success,
        String errorMessage,
        int candidateCount,
        List<ArticleCandidate> candidates,
        List<String> qualityWarnings
) {

    public static SourceCrawlResult success(BlogSource source, List<ArticleCandidate> candidates) {
        return new SourceCrawlResult(
                source.getCompanyKey(),
                source.getCompanyName(),
                true,
                null,
                candidates.size(),
                candidates,
                sourceWarnings(candidates)
        );
    }

    public static SourceCrawlResult failure(BlogSource source, String errorMessage) {
        return new SourceCrawlResult(
                source.getCompanyKey(),
                source.getCompanyName(),
                false,
                errorMessage,
                0,
                List.of(),
                List.of("SOURCE_FETCH_FAILED")
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
