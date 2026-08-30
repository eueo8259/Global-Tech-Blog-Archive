package com.globaltechblogarchive.crawl.collector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class SitemapArticleCandidateCollectorTest {

    @Test
    void collectBuildsCardsFromSitemapAndDetailMetadata() {
        BlogSource source = BlogSource.create(
                Company.create("anthropic", "Anthropic"),
                "anthropic-engineering",
                "Anthropic Engineering",
                "https://www.anthropic.com/engineering",
                "https://www.anthropic.com/sitemap.xml",
                CollectionMethod.SITEMAP
        );
        String articleLastModified = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1).toString();
        String productLastModified = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2).toString();
        String publishedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1).toString();
        String sitemap = """
                <urlset>
                  <url>
                    <loc>https://www.anthropic.com/engineering/building-effective-agents</loc>
                    <lastmod>%s</lastmod>
                  </url>
                  <url>
                    <loc>https://www.anthropic.com/news/product-launch</loc>
                    <lastmod>%s</lastmod>
                  </url>
                </urlset>
                """.formatted(articleLastModified, productLastModified);
        String detail = """
                <html>
                  <head>
                    <meta property="og:title" content="Building Effective AI Agents" />
                    <meta name="description" content="Practical notes for building reliable agents." />
                    <meta property="article:published_time" content="%s" />
                    <meta property="article:section" content="AI Research" />
                  </head>
                </html>
                """.formatted(publishedAt);
        SitemapArticleCandidateCollector collector = new SitemapArticleCandidateCollector(new StubClient(Map.of(
                "https://www.anthropic.com/sitemap.xml", sitemap,
                "https://www.anthropic.com/engineering/building-effective-agents", detail
        )));

        var cards = collector.collect(source, CrawlPolicy.recent());

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalTitle()).isEqualTo("Building Effective AI Agents");
        assertThat(cards.getFirst().originalUrl()).isEqualTo("https://www.anthropic.com/engineering/building-effective-agents");
        assertThat(cards.getFirst().publishedAt()).isNotNull();
        assertThat(cards.getFirst().shortContext()).contains("reliable agents");
        assertThat(cards.getFirst().categoryHint()).isEqualTo("AI Research");
    }

    @Test
    void recentModeFiltersByDetailPublishedAtAfterSitemapLastModifiedSelection() {
        BlogSource source = BlogSource.create(
                Company.create("anthropic", "Anthropic"),
                "anthropic-engineering",
                "Anthropic Engineering",
                "https://www.anthropic.com/engineering",
                "https://www.anthropic.com/sitemap.xml",
                CollectionMethod.SITEMAP
        );
        String lastModified = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1).toString();
        String oldPublishedAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10).toString();
        String sitemap = """
                <urlset>
                  <url>
                    <loc>https://www.anthropic.com/engineering/old-but-recently-modified</loc>
                    <lastmod>%s</lastmod>
                  </url>
                </urlset>
                """.formatted(lastModified);
        String detail = """
                <html>
                  <head>
                    <meta property="og:title" content="Old But Recently Modified" />
                    <meta property="article:published_time" content="%s" />
                  </head>
                </html>
                """.formatted(oldPublishedAt);
        SitemapArticleCandidateCollector collector = new SitemapArticleCandidateCollector(new StubClient(Map.of(
                "https://www.anthropic.com/sitemap.xml", sitemap,
                "https://www.anthropic.com/engineering/old-but-recently-modified", detail
        )));

        var cards = collector.collect(source, CrawlPolicy.recent());

        assertThat(cards).isEmpty();
    }

    @Test
    void backfillModeFetchesUpToFiftyDetailsAfterSortingSitemapEntries() {
        BlogSource source = BlogSource.create(
                Company.create("shopify", "Shopify"),
                "shopify",
                "Shopify Engineering",
                "https://shopify.engineering/",
                "https://shopify.engineering/sitemap.xml",
                CollectionMethod.SITEMAP
        );
        StringBuilder sitemap = new StringBuilder("<urlset>");
        Map<String, String> documents = new HashMap<>();
        List<String> expectedDetails = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            String url = "https://shopify.engineering/article-" + index;
            sitemap.append("<url><loc>").append(url).append("</loc><lastmod>")
                    .append(OffsetDateTime.now(ZoneOffset.UTC).minusDays(index))
                    .append("</lastmod></url>");
            String detail = index == 0
                    ? "<html><head></head></html>"
                    : "<html><head><title>Article " + index + "</title></head></html>";
            documents.put(url, detail);
            expectedDetails.add(url);
        }
        sitemap.append("</urlset>");
        documents.put("https://shopify.engineering/sitemap.xml", sitemap.toString());
        RecordingClient client = new RecordingClient(documents);
        SitemapArticleCandidateCollector collector = new SitemapArticleCandidateCollector(client);

        List<?> cards = collector.collect(source, CrawlPolicy.backfill(50));

        assertThat(cards).hasSize(29);
        assertThat(client.detailRequests()).containsExactlyElementsOf(expectedDetails);
    }

    private static class StubClient extends SourceDocumentClient {
        private final Map<String, String> documents;

        StubClient(Map<String, String> documents) {
            super(new SimpleMeterRegistry());
            this.documents = documents;
        }

        @Override
        public String fetch(String url) {
            return documents.get(url);
        }
    }

    private static class RecordingClient extends SourceDocumentClient {
        private final Map<String, String> documents;
        private final List<String> detailRequests = new ArrayList<>();

        RecordingClient(Map<String, String> documents) {
            super(new SimpleMeterRegistry());
            this.documents = documents;
        }

        @Override
        public String fetch(String url) {
            if (!url.endsWith("sitemap.xml")) {
                detailRequests.add(url);
            }
            return documents.get(url);
        }

        List<String> detailRequests() {
            return detailRequests;
        }
    }
}
