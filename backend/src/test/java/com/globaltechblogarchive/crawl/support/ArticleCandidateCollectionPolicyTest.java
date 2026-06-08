package com.globaltechblogarchive.crawl.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.parser.ParsedArticleCard;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleCandidateCollectionPolicyTest {

    @Test
    void applyKeepsRecentCandidatesOnlyAndLimitsToTwenty() {
        LocalDateTime now = LocalDateTime.now();
        List<ParsedArticleCard> cards = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            cards.add(card("recent-" + index, now.minusHours(index)));
        }
        cards.add(card("old", now.minusDays(3)));
        cards.add(new ParsedArticleCard("missing date", "https://example.com/missing-date", null, "missing date"));

        List<ParsedArticleCard> filtered = ArticleCandidateCollectionPolicy.apply(cards);

        assertThat(filtered).hasSize(20);
        assertThat(filtered).extracting(ParsedArticleCard::originalTitle)
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

    private ParsedArticleCard card(String title, LocalDateTime publishedAt) {
        return new ParsedArticleCard(title, "https://example.com/" + title, publishedAt, title);
    }
}
