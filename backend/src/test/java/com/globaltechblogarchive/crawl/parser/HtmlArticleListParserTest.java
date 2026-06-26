package com.globaltechblogarchive.crawl.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.helper.ArticleListParserPropertiesFixture;
import com.globaltechblogarchive.crawl.parser.config.ArticleListParserProperties;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HtmlArticleListParserTest {

    private final HtmlArticleListParser parser = new HtmlArticleListParser(ArticleListParserPropertiesFixture.full());


    @ParameterizedTest
    @MethodSource("htmlSources")
    void parseExtractsCompanyArticleCards(String sourceKey, String siteUrl, String articleUrl, String signal) {
        BlogSource source = source(sourceKey, siteUrl);
        String html = """
                <main>
                  <nav><a href="/careers">Jobs</a><a href="/feed">RSS</a></nav>
                  <article class="article-card">
                    <span class="category-label">%s</span>
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
        assertThat(cards.getFirst().categoryHint()).isEqualTo(signal);
    }

    @org.junit.jupiter.api.Test
    void parseFallsBackToArticleLinksWhenCardBlocksAreMissing() {
        BlogSource source = source(
                "discord",
                "https://discord.com/category/engineering"
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
        assertThat(cards).extracting(ParsedArticle::originalUrl)
                .containsExactly(
                        "https://discord.com/blog/how-discord-automates-scylladb-clusters-at-scale",
                        "https://discord.com/blog/how-discord-indexes-trillions-of-messages"
                );
    }

    @org.junit.jupiter.api.Test
    void parseCleansHtmlEntitiesFromArticleUrls() {
        BlogSource source = source(
                "doordash",
                "https://careersatdoordash.com/career-areas/engineering/"
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

    @org.junit.jupiter.api.Test
    void parseExcludesUberCategoryLinks() {
        BlogSource source = source(
                "uber",
                "https://www.uber.com/blog/engineering"
        );
        String html = """
                <main>
                  <a href="/kr/en/blog/advertising/">
                    Advertising Learn more about advertising on Uber.
                  </a>
                  <a href="/kr/en/blog/engineering/">
                    Engineering The technology behind Uber Engineering
                  </a>
                  <a href="/kr/en/blog/scaling-real-time-traffic/">
                    Scaling Real-Time Traffic Forecasting with a Graph-Aware Transformer
                  </a>
                </main>
                """;

        var cards = parser.parse(source, html);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalUrl())
                .isEqualTo("https://www.uber.com/kr/en/blog/scaling-real-time-traffic/");
    }

    @org.junit.jupiter.api.Test
    void parseExcludesUnexpectedHostLinks() {
        BlogSource source = source(
                "stripe",
                "https://stripe.com/blog/engineering"
        );
        String html = """
                <main>
                  <a href="https://stripe.events/acnext_seattle">
                    The future of agentic commerce is here
                  </a>
                  <a href="/blog/how-we-built-real-time-analytics">
                    How we built real-time analytics for Stripe Billing
                  </a>
                </main>
                """;

        var cards = parser.parse(source, html);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalUrl())
                .isEqualTo("https://stripe.com/blog/how-we-built-real-time-analytics");
    }

    @org.junit.jupiter.api.Test
    void parseExtractsStripeDevDotSeparatedListDate() {
        BlogSource source = source(
                "stripe",
                "https://stripe.dev/blog"
        );
        String html = """
                <main>
                  <section class="blog-list">
                    <a href="/blog/modern-java-at-stripe-language-upgrades-as-a-service">
                      2026.5.27 Modern Java at Stripe: Language upgrades as a service
                    </a>
                    <p>Summary: How Stripe upgrades Java across a large JVM codebase.</p>
                    <p>Topic: Engineering</p>
                  </section>
                </main>
                """;

        var cards = parser.parse(source, html);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalUrl())
                .isEqualTo("https://stripe.dev/blog/modern-java-at-stripe-language-upgrades-as-a-service");
        assertThat(cards.getFirst().publishedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 5, 27, 0, 0));
    }

    static Stream<Arguments> htmlSources() {
        return Stream.of(
                Arguments.of("openai", "https://openai.com/news/", "/news/engineering-systems", "Engineering"),
                Arguments.of("anthropic-engineering", "https://www.anthropic.com/news", "/news/research-systems", "Research"),
                Arguments.of("figma", "https://www.figma.com/blog/engineering/", "/blog/realtime-engineering", "Engineering"),
                Arguments.of("uber", "https://www.uber.com/blog/engineering", "/blog/realtime-platform", "Engineering"),
                Arguments.of("airbnb", "https://airbnb.tech/", "https://medium.com/airbnb-engineering/platform", "Engineering"),
                Arguments.of("stripe", "https://stripe.com/blog/engineering", "/blog/database-systems", "Engineering"),
                Arguments.of("cloudflare", "https://blog.cloudflare.com/", "/networking-at-edge", "Networking"),
                Arguments.of("doordash", "https://careersatdoordash.com/career-areas/engineering/", "/engineering-blog/backend-platform", "Backend"),
                Arguments.of("discord", "https://discord.com/category/engineering", "/blog/realtime-engineering", "Developers"),
                Arguments.of("shopify", "https://shopify.engineering/", "/database-at-scale", "Engineering"),
                Arguments.of("datadog", "https://www.datadoghq.com/blog/engineering/", "/blog/observability-platform", "Engineering"),
                Arguments.of("amazon-science", "https://www.amazon.science/blog", "/blog/ai-systems", "AI")
        );
    }

    private BlogSource source(String sourceKey, String siteUrl) {
        return BlogSource.create(
                Company.create(sourceKey, sourceKey),
                sourceKey,
                sourceKey,
                siteUrl,
                null,
                CollectionMethod.HTML_SCRAPING
        );
    }
}
