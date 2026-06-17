package com.globaltechblogarchive.crawl.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleCandidateCollectionPolicyTest {

    @Test
    void applyKeepsRecentCandidatesOnlyAndLimitsToTwenty() {
        LocalDateTime now = LocalDateTime.now();
        List<ParsedArticle> cards = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            cards.add(card("recent-" + index, now.minusHours(index)));
        }
        cards.add(card("old", now.minusDays(3)));
        cards.add(new ParsedArticle("missing date", "https://example.com/missing-date", null, "missing date"));

        List<ParsedArticle> filtered = ArticleCandidateCollectionPolicy.apply(cards, CrawlMode.RECENT);

        assertThat(filtered).hasSize(20);
        assertThat(filtered).extracting(ParsedArticle::originalTitle)
                .containsExactly(
                        "recent-0",
                        "recent-1",
                        "recent-2",
                        "recent-3",
                        "recent-4",
                        "recent-5",
                        "recent-6",
                        "recent-7",
                        "recent-8",
                        "recent-9",
                        "recent-10",
                        "recent-11",
                        "recent-12",
                        "recent-13",
                        "recent-14",
                        "recent-15",
                        "recent-16",
                        "recent-17",
                        "recent-18",
                        "recent-19"
                );
    }

    @Test
    void applyKeepsMissingDateCandidatesInRecentMode() {
        LocalDateTime now = LocalDateTime.now();
        List<ParsedArticle> cards = List.of(
                card("recent", now.minusHours(1)),
                card("old", now.minusDays(3)),
                card("missing", null)
        );

        List<ParsedArticle> filtered = ArticleCandidateCollectionPolicy.apply(cards, CrawlMode.RECENT);

        assertThat(filtered).extracting(ParsedArticle::originalTitle)
                .containsExactly("recent", "missing");
    }

    @Test
    void backfillModeSortsDatedCandidatesFirstAndUsesMissingDatesAsFallback() {
        LocalDateTime now = LocalDateTime.now();
        List<ParsedArticle> cards = List.of(
                card("missing-first", null),
                card("older", now.minusDays(30)),
                card("newest", now.minusDays(5)),
                card("missing-second", null)
        );

        List<ParsedArticle> filtered = ArticleCandidateCollectionPolicy.apply(cards, CrawlMode.BACKFILL);

        assertThat(filtered).extracting(ParsedArticle::originalTitle)
                .containsExactly("newest", "older", "missing-first", "missing-second");
    }

    @Test
    void backfillModeLimitsCandidatesToFiftyBeforeMissingDateFallback() {
        LocalDateTime now = LocalDateTime.now();
        List<ParsedArticle> cards = new ArrayList<>();
        cards.add(card("missing", null));
        for (int index = 0; index < 50; index++) {
            cards.add(card("dated-" + index, now.minusHours(index)));
        }

        List<ParsedArticle> filtered = ArticleCandidateCollectionPolicy.apply(cards, CrawlMode.BACKFILL);

        assertThat(filtered).hasSize(50);
        assertThat(filtered).extracting(ParsedArticle::originalTitle).doesNotContain("missing");
    }

    private ParsedArticle card(String title, LocalDateTime publishedAt) {
        return new ParsedArticle(title, "https://example.com/" + title, publishedAt, title);
    }
}
