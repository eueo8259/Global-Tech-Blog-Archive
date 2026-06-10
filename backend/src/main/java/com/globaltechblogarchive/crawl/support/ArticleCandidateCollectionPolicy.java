package com.globaltechblogarchive.crawl.support;

import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class ArticleCandidateCollectionPolicy {

    public static final int RECENT_WINDOW_DAYS = 2;
    public static final int MAX_CANDIDATES = 20;

    private ArticleCandidateCollectionPolicy() {
    }

    public static List<ParsedArticle> apply(List<ParsedArticle> cards) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RECENT_WINDOW_DAYS);
        return cards.stream()
                .filter(card -> card.publishedAt() != null)
                .filter(card -> !card.publishedAt().isBefore(threshold))
                .sorted(Comparator.comparing(ParsedArticle::publishedAt).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }
}
