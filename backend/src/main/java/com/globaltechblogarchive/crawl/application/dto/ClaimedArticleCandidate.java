package com.globaltechblogarchive.crawl.application.dto;

import java.time.LocalDateTime;

public record ClaimedArticleCandidate(
        Long id,
        Long companyId,
        Long sourceId,
        String articleUrl,
        String articleUrlHash,
        String originalTitle,
        String shortContext,
        String categoryHint,
        LocalDateTime publishedAt,
        int attemptCount,
        String promptVersion,
        LocalDateTime claimedAt
) {
}
