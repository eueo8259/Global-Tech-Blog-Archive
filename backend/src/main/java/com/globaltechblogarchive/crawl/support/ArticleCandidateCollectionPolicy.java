package com.globaltechblogarchive.crawl.support;

import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class ArticleCandidateCollectionPolicy {

    public static final int RECENT_WINDOW_DAYS = 2;
    private ArticleCandidateCollectionPolicy() {
    }

    public static List<ParsedArticle> apply(List<ParsedArticle> cards, CrawlPolicy policy) {
        return select(cards, policy, ParsedArticle::publishedAt);
    }

    public static <T> List<T> select(
            List<T> candidates,
            CrawlPolicy policy,
            Function<T, LocalDateTime> dateExtractor
    ) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RECENT_WINDOW_DAYS);
        return candidates.stream()
                .filter(candidate -> policy.mode() == CrawlMode.BACKFILL
                        || isRecentOrMissingDate(dateExtractor.apply(candidate), threshold))
                .sorted(Comparator.comparing(
                        dateExtractor,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(policy.maxCandidatesPerSource())
                .toList();
    }

    private static boolean isRecentOrMissingDate(LocalDateTime publishedAt, LocalDateTime threshold) {
        return publishedAt == null || !publishedAt.isBefore(threshold);
    }
}
