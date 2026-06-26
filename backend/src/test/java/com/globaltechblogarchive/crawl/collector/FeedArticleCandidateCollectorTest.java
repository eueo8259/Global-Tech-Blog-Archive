package com.globaltechblogarchive.crawl.collector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import org.junit.jupiter.api.Test;

class FeedArticleCandidateCollectorTest {

    private final FeedArticleCandidateCollector collector = new FeedArticleCandidateCollector(null);

    @Test
    void parseRssExtractsArticleCandidates() {
        BlogSource source = source(CollectionMethod.RSS);
        String rss = """
                <rss><channel>
                  <item>
                    <title>Scaling Java services</title>
                    <link>/engineering/scaling-java</link>
                    <pubDate>Mon, 01 Jun 2026 10:00:00 GMT</pubDate>
                    <category>Backend</category>
                    <category><![CDATA[Production Engineering]]></category>
                    <description><![CDATA[<p>How the platform team scaled backend services.</p>]]></description>
                  </item>
                </channel></rss>
                """;

        var cards = collector.parse(source, rss);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalTitle()).isEqualTo("Scaling Java services");
        assertThat(cards.getFirst().originalUrl()).isEqualTo("https://example.com/engineering/scaling-java");
        assertThat(cards.getFirst().publishedAt()).isEqualTo("2026-06-01T10:00:00");
        assertThat(cards.getFirst().shortContext()).contains("platform team");
        assertThat(cards.getFirst().categoryHint()).isEqualTo("Backend, Production Engineering");
    }

    @Test
    void parseAtomExtractsArticleCandidates() {
        BlogSource source = source(CollectionMethod.ATOM);
        String atom = """
                <feed>
                  <entry>
                    <title>Realtime collaboration architecture</title>
                    <link rel="alternate" href="https://example.com/blog/realtime" />
                    <published>2026-06-02T11:30:00Z</published>
                    <category term="Frontend" />
                    <summary>Architecture notes from the editor team.</summary>
                  </entry>
                </feed>
                """;

        var cards = collector.parse(source, atom);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalTitle()).isEqualTo("Realtime collaboration architecture");
        assertThat(cards.getFirst().originalUrl()).isEqualTo("https://example.com/blog/realtime");
        assertThat(cards.getFirst().shortContext()).isEqualTo("Architecture notes from the editor team.");
        assertThat(cards.getFirst().categoryHint()).isEqualTo("Frontend");
    }

    private BlogSource source(CollectionMethod method) {
        return BlogSource.create(
                Company.create("example", "Example"),
                "example",
                "Example Blog",
                "https://example.com/blog/",
                "https://example.com/feed",
                method
        );
    }
}

