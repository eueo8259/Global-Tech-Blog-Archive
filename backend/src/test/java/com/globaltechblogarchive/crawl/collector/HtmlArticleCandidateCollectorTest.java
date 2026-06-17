package com.globaltechblogarchive.crawl.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.collector.impl.HtmlArticleCandidateCollector;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.helper.ArticleListParserPropertiesFixture;
import com.globaltechblogarchive.crawl.parser.ArticleListParserRegistry;
import com.globaltechblogarchive.crawl.parser.HtmlArticleListParser;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HtmlArticleCandidateCollectorTest {

    @Test
    void discordUsesDetailTitleAndVisiblePublicationDate() {
        BlogSource source = discordSource();
        String articleUrl = "https://discord.com/blog/updated-requirements-to-how-apps-access-data-in-servers";
        String list = """
                <main>
                  <a href="/blog/updated-requirements-to-how-apps-access-data-in-servers">
                    Engineering & Developers
                    Updated Requirements to How Apps Access Data in Servers
                    Discord is updating requirements and requiring annual review.
                  </a>
                </main>
                """;
        String detail = """
                <html>
                  <head>
                    <meta property="og:title" content="Wrong fallback title" />
                    <meta name="description" content="Discord is updating requirements." />
                    <script type="application/ld+json">
                      {"headline":"JSON-LD fallback title","datePublished":"Jun 11, 2026"}
                    </script>
                  </head>
                  <body>
                    <div>Engineering & Developers</div>
                    <h1>Updated Requirements to How Apps Access Data in Servers</h1>
                    <div>Tom Jacques</div>
                    <div>June 10, 2026</div>
                  </body>
                </html>
                """;
        RecordingClient client = new RecordingClient(Map.of(
                source.getSiteUrl(), list,
                articleUrl, detail
        ));
        HtmlArticleCandidateCollector collector = collector(client);

        var articles = collector.collect(source, CrawlMode.INITIAL);

        assertThat(articles).hasSize(1);
        assertThat(articles.getFirst().originalTitle())
                .isEqualTo("Updated Requirements to How Apps Access Data in Servers");
        assertThat(articles.getFirst().publishedAt()).isEqualTo(LocalDateTime.of(2026, 6, 10, 0, 0));
        assertThat(articles.getFirst().shortContext()).isEqualTo("Discord is updating requirements.");
    }

    @Test
    void discordFetchesAtMostTwentyDetailPages() {
        BlogSource source = discordSource();
        StringBuilder list = new StringBuilder("<main>");
        Map<String, String> documents = new HashMap<>();
        List<String> expectedDetails = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            String path = "/blog/article-" + index;
            String url = "https://discord.com" + path;
            list.append("<a href=\"").append(path).append("\">Engineering & Developers Article ")
                    .append(index).append(" with a sufficiently long title</a>");
            documents.put(url, "<h1>Article " + index + " with a sufficiently long title</h1>"
                    + "<div>June 10, 2026</div>");
            if (index < 20) {
                expectedDetails.add(url);
            }
        }
        list.append("</main>");
        documents.put(source.getSiteUrl(), list.toString());
        RecordingClient client = new RecordingClient(documents);

        var articles = collector(client).collect(source, CrawlMode.INITIAL);

        assertThat(articles).hasSize(20);
        assertThat(client.detailRequests()).containsExactlyElementsOf(expectedDetails);
        assertThat(client.detailRequests()).doesNotContain("https://discord.com/blog/article-20");
    }

    @Test
    void uberInitialCollectsFirstTwoListPages() {
        BlogSource source = uberSource();
        String pageOne = """
                <main>
                  <a href="/kr/en/blog/advertising/">
                    Advertising Learn more about advertising on Uber.
                  </a>
                  <a href="/kr/en/blog/scaling-real-time-traffic/">
                    Scaling Real-Time Traffic Forecasting with a Graph-Aware Transformer
                  </a>
                  <time>June 16, 2026</time>
                </main>
                """;
        String pageTwo = """
                <main>
                  <a href="/kr/en/blog/junit-migration/">
                    How Uber Executed A JUnit Migration at Massive Scale
                  </a>
                  <a href="/kr/en/blog/scaling-real-time-traffic/">
                    Scaling Real-Time Traffic Forecasting with a Graph-Aware Transformer
                  </a>
                </main>
                """;
        RecordingClient client = new RecordingClient(Map.of(
                source.getSiteUrl(), pageOne,
                "https://www.uber.com/blog/engineering/page/2", pageTwo
        ));

        var articles = collector(client).collect(source, CrawlMode.INITIAL);

        assertThat(articles).extracting(ParsedArticle::originalTitle)
                .containsExactly(
                        "Scaling Real-Time Traffic Forecasting with a Graph-Aware Transformer",
                        "How Uber Executed A JUnit Migration at Massive Scale"
                );
        assertThat(client.listRequests()).containsExactly(
                source.getSiteUrl(),
                "https://www.uber.com/blog/engineering/page/2"
        );
    }

    @Test
    void uberRecentCollectsOnlyFirstListPage() {
        BlogSource source = uberSource();
        String pageOne = """
                <main>
                  <a href="/kr/en/blog/scaling-real-time-traffic/">
                    Scaling Real-Time Traffic Forecasting with a Graph-Aware Transformer
                  </a>
                </main>
                """;
        RecordingClient client = new RecordingClient(Map.of(source.getSiteUrl(), pageOne));

        collector(client).collect(source, CrawlMode.RECENT);

        assertThat(client.listRequests()).containsExactly(source.getSiteUrl());
    }

    @Test
    void stripeInitialCollectsEngineeringCardsWithVisibleDates() {
        BlogSource source = stripeSource();
        String list = """
                <main>
                  <article class="BlogIndexPost">
                    <span>Engineering</span>
                    <a href="/blog/how-we-built-it-real-time-analytics-for-stripe-billing">
                      How we built it: Real-time analytics for Stripe Billing
                    </a>
                    <span>March 17, 2025</span>
                    <p>How Stripe built real-time analytics for billing data.</p>
                  </article>
                </main>
                """;
        RecordingClient client = new RecordingClient(Map.of(source.getSiteUrl(), list));

        var articles = collector(client).collect(source, CrawlMode.INITIAL);

        assertThat(articles).hasSize(1);
        assertThat(articles.getFirst().originalTitle())
                .isEqualTo("How we built it: Real-time analytics for Stripe Billing");
        assertThat(articles.getFirst().originalUrl())
                .isEqualTo("https://stripe.com/blog/how-we-built-it-real-time-analytics-for-stripe-billing");
        assertThat(articles.getFirst().publishedAt()).isEqualTo(LocalDateTime.of(2025, 3, 17, 0, 0));
        assertThat(articles.getFirst().shortContext()).contains("real-time analytics");
    }

    private HtmlArticleCandidateCollector collector(SourceDocumentClient client) {
        return new HtmlArticleCandidateCollector(
                client,
                new ArticleListParserRegistry(List.of(
                        new HtmlArticleListParser(ArticleListParserPropertiesFixture.full())
                ))
        );
    }

    private BlogSource discordSource() {
        return BlogSource.create(
                Company.create("discord", "Discord"),
                "discord",
                "Discord Engineering",
                "https://discord.com/category/engineering",
                null,
                CollectionMethod.HTML_SCRAPING
        );
    }

    private BlogSource uberSource() {
        return BlogSource.create(
                Company.create("uber", "Uber"),
                "uber",
                "Uber Engineering Blog",
                "https://www.uber.com/blog/engineering",
                null,
                CollectionMethod.HTML_SCRAPING
        );
    }

    private BlogSource stripeSource() {
        return BlogSource.create(
                Company.create("stripe", "Stripe"),
                "stripe",
                "Stripe Engineering Blog",
                "https://stripe.com/blog/engineering",
                null,
                CollectionMethod.HTML_SCRAPING
        );
    }

    private static class RecordingClient extends SourceDocumentClient {
        private final Map<String, String> documents;
        private final List<String> listRequests = new ArrayList<>();
        private final List<String> detailRequests = new ArrayList<>();

        RecordingClient(Map<String, String> documents) {
            this.documents = documents;
        }

        @Override
        public String fetch(String url) {
            if (url.contains("/category/") || url.contains("/engineering")) {
                listRequests.add(url);
            } else {
                detailRequests.add(url);
            }
            return documents.get(url);
        }

        List<String> listRequests() {
            return listRequests;
        }

        List<String> detailRequests() {
            return detailRequests;
        }
    }
}
