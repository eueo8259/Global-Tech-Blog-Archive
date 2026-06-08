package com.globaltechblogarchive.crawl.api.dto;

import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ArticleCandidateResponse(
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

    public static ArticleCandidateResponse from(ArticleCandidate candidate) {
        return new ArticleCandidateResponse(
                candidate.companyKey(),
                candidate.companyName(),
                candidate.originalTitle(),
                candidate.originalUrl(),
                candidate.publishedAt(),
                candidate.shortContext(),
                candidate.normalizedUrl(),
                candidate.normalizedUrlHash(),
                candidate.duplicate(),
                candidate.decisionStatus(),
                candidate.validationWarnings()
        );
    }
}
