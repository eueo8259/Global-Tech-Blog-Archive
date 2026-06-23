package com.globaltechblogarchive.crawl.api.dto;

import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ArticleCandidateResponse(
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

    public static ArticleCandidateResponse from(ArticleCandidate candidate) {
        return new ArticleCandidateResponse(
                candidate.companyKey(),
                candidate.companyName(),
                candidate.originalTitle(),
                candidate.articleUrl(),
                candidate.publishedAt(),
                candidate.shortContext(),
                candidate.articleUrlHash(),
                candidate.duplicate(),
                candidate.decisionStatus(),
                candidate.validationWarnings()
        );
    }
}
