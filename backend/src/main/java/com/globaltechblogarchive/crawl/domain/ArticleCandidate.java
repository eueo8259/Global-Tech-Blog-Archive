package com.globaltechblogarchive.crawl.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleCandidate(
        String companyKey,
        String companyName,
        String originalTitle,
        String articleUrl,
        LocalDateTime publishedAt,
        String shortContext,
        String articleUrlHash,
        boolean duplicate,
        ArticleCandidateDecisionStatus decisionStatus,
        List<String> validationWarnings
) {

    public ArticleCandidate withDecisionStatus(ArticleCandidateDecisionStatus decisionStatus) {
        return new ArticleCandidate(
                companyKey,
                companyName,
                originalTitle,
                articleUrl,
                publishedAt,
                shortContext,
                articleUrlHash,
                duplicate,
                decisionStatus,
                validationWarnings
        );
    }
}
