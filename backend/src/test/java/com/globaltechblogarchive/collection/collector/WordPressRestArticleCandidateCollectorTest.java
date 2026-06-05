package com.globaltechblogarchive.collection.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WordPressRestArticleCandidateCollectorTest {

    private final WordPressRestArticleCandidateCollector collector =
            new WordPressRestArticleCandidateCollector(null, new ObjectMapper());

    @Test
    void parseReturnsArticleCardsFromWordPressRestPosts() {
        String json = """
                [
                  {
                    "date": "2026-06-01T15:52:16",
                    "link": "https://careersatdoordash.com/blog/doordashs-one-click-simulation-and-evaluation-platform-for-support-chatbots/",
                    "title": {
                      "rendered": "Inside DoorDash&#8217;s one-click simulation and evaluation platform for support chatbots"
                    },
                    "excerpt": {
                      "rendered": "<p>Shipping high-quality support chatbots is an end-to-end problem.</p>"
                    }
                  }
                ]
                """;

        var cards = collector.parse(json);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalTitle())
                .isEqualTo("Inside DoorDash s one-click simulation and evaluation platform for support chatbots");
        assertThat(cards.getFirst().originalUrl())
                .isEqualTo("https://careersatdoordash.com/blog/doordashs-one-click-simulation-and-evaluation-platform-for-support-chatbots/");
        assertThat(cards.getFirst().publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 15, 52, 16));
        assertThat(cards.getFirst().shortContext())
                .isEqualTo("Shipping high-quality support chatbots is an end-to-end problem.");
    }
}
