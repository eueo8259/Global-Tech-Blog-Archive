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
class RealSitemapArticleCandidateCollectorTest {

    private final SitemapArticleCandidateCollector collector =
            new SitemapArticleCandidateCollector(new SourceDocumentClient());

    @ParameterizedTest(name = "{0} sitemap returns article cards")
    @MethodSource("sitemapSources")
    void collectFromRealSitemapReturnsArticleCards(
            String companyKey,
            String companyName,
            String siteUrl,
            String sitemapUrl
    ) {
        BlogSource source = BlogSource.create(companyKey, companyName, siteUrl, sitemapUrl, CollectionMethod.SITEMAP);

        List<ParsedArticleCard> cards = collector.collect(source);

        assertThat(cards).isNotEmpty();
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.originalTitle()).isNotBlank();
            assertThat(card.originalUrl()).startsWith("http");
            assertThat(card.publishedAt()).isNotNull();
            assertThat(card.shortContext()).isNotBlank();
        });
    }

    private static Stream<Arguments> sitemapSources() {
        return Stream.of(
                Arguments.of("anthropic", "Anthropic", "https://www.anthropic.com/engineering", "https://www.anthropic.com/sitemap.xml"),
                Arguments.of("shopify", "Shopify", "https://shopify.engineering/", "https://shopify.engineering/sitemap.xml")
        );
    }
}
