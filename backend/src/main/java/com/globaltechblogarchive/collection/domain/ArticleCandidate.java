package com.globaltechblogarchive.collection.domain;

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
        List<String> validationWarnings
) {
}
