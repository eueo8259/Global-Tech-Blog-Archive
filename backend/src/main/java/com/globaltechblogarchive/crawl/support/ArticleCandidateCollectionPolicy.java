package com.globaltechblogarchive.crawl.support;

import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class ArticleCandidateCollectionPolicy {

    public static final int RECENT_WINDOW_DAYS = 2;
    public static final int RECENT_MAX_CANDIDATES = 20;
    public static final int BACKFILL_MAX_CANDIDATES = 50;

    private ArticleCandidateCollectionPolicy() {
    }

    public static List<ParsedArticle> apply(List<ParsedArticle> cards, CrawlMode mode) {
        return select(cards, mode, ParsedArticle::publishedAt);
    }

    public static <T> List<T> select(List<T> candidates, CrawlMode mode, Function<T, LocalDateTime> dateExtractor) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RECENT_WINDOW_DAYS);
        return candidates.stream()
                .filter(candidate -> mode == CrawlMode.BACKFILL || isRecentOrMissingDate(dateExtractor.apply(candidate), threshold))
                .sorted(Comparator.comparing(
                        dateExtractor,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(maxCandidates(mode))
                .toList();
    }

    public static int maxCandidates(CrawlMode mode) {
        if (mode == CrawlMode.BACKFILL) {
            return BACKFILL_MAX_CANDIDATES;
        }
        return RECENT_MAX_CANDIDATES;
    }

    private static boolean isRecentOrMissingDate(LocalDateTime publishedAt, LocalDateTime threshold) {
        return publishedAt == null || !publishedAt.isBefore(threshold);
    }
}
