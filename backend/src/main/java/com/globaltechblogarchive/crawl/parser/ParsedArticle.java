package com.globaltechblogarchive.crawl.parser;

import java.time.LocalDateTime;

public record ParsedArticle(
        String originalTitle,
        String originalUrl,
        LocalDateTime publishedAt,
        String shortContext,
        String categoryHint
) {

    public ParsedArticle(
            String originalTitle,
            String originalUrl,
            LocalDateTime publishedAt,
            String shortContext
    ) {
        this(originalTitle, originalUrl, publishedAt, shortContext, null);
    }
}

