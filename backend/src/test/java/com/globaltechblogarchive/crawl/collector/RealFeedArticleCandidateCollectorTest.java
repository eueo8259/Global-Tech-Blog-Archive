package com.globaltechblogarchive.crawl.collector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.parser.ParsedArticleCard;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("real")
class RealFeedArticleCandidateCollectorTest {

    private final FeedArticleCandidateCollector collector =
            new FeedArticleCandidateCollector(new SourceDocumentClient());

    @ParameterizedTest(name = "{0} feed returns parseable article cards")
    @MethodSource("feedSources")
    void collectFromRealFeedReturnsArticleCards(
            String companyKey,
            CollectionMethod method,
            String siteUrl,
            String feedUrl
    ) {
        BlogSource source = BlogSource.create(companyKey, companyKey, siteUrl, feedUrl, method);

        List<ParsedArticleCard> cards = collector.collect(source);

        assertThat(cards).isNotEmpty();
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.originalTitle()).isNotBlank();
            assertThat(card.originalUrl()).startsWith("http");
            assertThat(card.publishedAt()).isNotNull();
            assertThat(card.shortContext()).isNotNull();
        });
    }

    private static Stream<Arguments> feedSources() {
        return Stream.of(
                Arguments.of("openai", CollectionMethod.RSS, "https://openai.com/news/", "https://openai.com/news/rss.xml"),
                Arguments.of("figma", CollectionMethod.ATOM, "https://www.figma.com/blog/engineering/", "https://www.figma.com/blog/feed/atom.xml"),
                Arguments.of("stripe", CollectionMethod.RSS, "https://stripe.com/blog/engineering", "https://stripe.com/blog/feed.rss"),
                Arguments.of("cloudflare", CollectionMethod.RSS, "https://blog.cloudflare.com/", "https://blog.cloudflare.com/tag/engineering/rss/"),
                Arguments.of("datadog", CollectionMethod.RSS, "https://www.datadoghq.com/blog/engineering/", "https://www.datadoghq.com/blog/engineering/index.xml"),
                Arguments.of("amazon-science", CollectionMethod.RSS, "https://www.amazon.science/blog", "https://www.amazon.science/index.rss")
        );
    }
}
