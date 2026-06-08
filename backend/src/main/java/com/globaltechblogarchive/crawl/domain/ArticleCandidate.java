package com.globaltechblogarchive.crawl.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleCandidate(
        String companyKey,
        String companyName,
        String originalTitle,
        String originalUrl,
        LocalDateTime publishedAt,
        String shortContext,
        String normalizedUrl,
        String normalizedUrlHash,
        boolean duplicate,
        ArticleCandidateDecisionStatus decisionStatus,
        List<String> validationWarnings
) {

    public ArticleCandidate withDecisionStatus(ArticleCandidateDecisionStatus decisionStatus) {
        return new ArticleCandidate(
                companyKey,
                companyName,
                originalTitle,
                originalUrl,
                publishedAt,
                shortContext,
                normalizedUrl,
                normalizedUrlHash,
                duplicate,
                decisionStatus,
                validationWarnings
        );
    }
}
