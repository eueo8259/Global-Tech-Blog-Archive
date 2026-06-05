package com.globaltechblogarchive.collection.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfiguredArticleListParserTest {

    private final ConfiguredArticleListParser parser = new ConfiguredArticleListParser();

    @ParameterizedTest
    @MethodSource("htmlSources")
    void parseExtractsCompanyArticleCards(String companyKey, String siteUrl, String articleUrl, String signal) {
        BlogSource source = BlogSource.create(companyKey, companyKey, siteUrl, null, CollectionMethod.HTML_SCRAPING);
        String html = """
                <main>
                  <nav><a href="/careers">Jobs</a><a href="/feed">RSS</a></nav>
                  <article class="article-card">
                    <span>%s</span>
                    <time datetime="2026-06-01">June 1, 2026</time>
                    <a href="%s">How we improved engineering systems</a>
                    <p>Deep notes about %s and production lessons.</p>
                  </article>
                  <footer><a href="https://twitter.com/example">Social</a></footer>
                </main>
                """.formatted(signal, articleUrl, signal);

        var cards = parser.parse(source, html);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalTitle()).isEqualTo("How we improved engineering systems");
        assertThat(cards.getFirst().originalUrl()).startsWith("http");
        assertThat(cards.getFirst().shortContext()).isNotBlank();
        assertThat(cards.getFirst().shortContext()).hasSizeLessThanOrEqualTo(500);
    }

    @org.junit.jupiter.api.Test
    void parseFallsBackToArticleLinksWhenCardBlocksAreMissing() {
        BlogSource source = BlogSource.create(
                "discord",
                "Discord",
                "https://discord.com/category/engineering",
                null,
                CollectionMethod.HTML_SCRAPING
        );
        String html = """
                <main>
                  <h1>Engineering & Developers</h1>
                  <a href="/category/engineering">Engineering & Developers</a>
                  <a href="/blog/how-discord-automates-scylladb-clusters-at-scale">
                    Engineering & Developers How Discord Automates ScyllaDB Clusters at Scale
                  </a>
                  <a href="/blog/how-discord-indexes-trillions-of-messages">
                    Engineering & Developers How Discord Indexes Trillions of Messages
                  </a>
                </main>
                """;

        var cards = parser.parse(source, html);

        assertThat(cards).hasSize(2);
        assertThat(cards).extracting(ParsedArticleCard::originalUrl)
                .containsExactly(
                        "https://discord.com/blog/how-discord-automates-scylladb-clusters-at-scale",
                        "https://discord.com/blog/how-discord-indexes-trillions-of-messages"
                );
    }

    @org.junit.jupiter.api.Test
    void parseCleansHtmlEntitiesFromArticleUrls() {
        BlogSource source = BlogSource.create(
                "doordash",
                "DoorDash",
                "https://careersatdoordash.com/career-areas/engineering/",
                null,
                CollectionMethod.HTML_SCRAPING
        );
        String html = """
                <main>
                  <a href="/blog/doordash-clusterless-ml-feature-store/?utm_source=engineering&#038;ref=list">
                    Lessons learned building DoorDash's clusterless ML feature store
                  </a>
                </main>
                """;

        var cards = parser.parse(source, html);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalUrl())
                .isEqualTo("https://careersatdoordash.com/blog/doordash-clusterless-ml-feature-store/?utm_source=engineering&ref=list");
    }

    static Stream<Arguments> htmlSources() {
        return Stream.of(
                Arguments.of("openai", "https://openai.com/news/", "/news/engineering-systems", "Engineering"),
                Arguments.of("anthropic", "https://www.anthropic.com/news", "/news/research-systems", "Research"),
                Arguments.of("figma", "https://www.figma.com/blog/engineering/", "/blog/realtime-engineering", "Engineering"),
                Arguments.of("uber", "https://www.uber.com/blog/engineering", "/blog/realtime-platform", "Engineering"),
                Arguments.of("airbnb", "https://airbnb.tech/", "https://medium.com/airbnb-engineering/platform", "Engineering"),
                Arguments.of("stripe", "https://stripe.com/blog/engineering", "/blog/database-systems", "Engineering"),
                Arguments.of("cloudflare", "https://blog.cloudflare.com/", "/networking-at-edge", "Networking"),
                Arguments.of("linkedin", "https://engineering.linkedin.com/content/engineering/en-us/blog", "/blog/data-systems", "Engineering"),
                Arguments.of("doordash", "https://careersatdoordash.com/career-areas/engineering/", "/engineering-blog/backend-platform", "Backend"),
                Arguments.of("discord", "https://discord.com/category/engineering", "/blog/realtime-engineering", "Developers"),
                Arguments.of("shopify", "https://shopify.engineering/", "/database-at-scale", "Engineering"),
                Arguments.of("datadog", "https://www.datadoghq.com/blog/engineering/", "/blog/observability-platform", "Engineering"),
                Arguments.of("amazon-science", "https://www.amazon.science/blog", "/blog/ai-systems", "AI")
        );
    }
}
