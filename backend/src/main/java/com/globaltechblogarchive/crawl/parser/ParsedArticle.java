package com.globaltechblogarchive.crawl.parser;

import java.time.LocalDateTime;

public record ParsedArticle(
        String originalTitle,
        String originalUrl,
        LocalDateTime publishedAt,
        String shortContext
) {
}

