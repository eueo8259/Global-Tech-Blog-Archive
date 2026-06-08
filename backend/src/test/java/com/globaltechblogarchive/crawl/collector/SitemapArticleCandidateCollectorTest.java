package com.globaltechblogarchive.crawl.collector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SitemapArticleCandidateCollectorTest {

    @Test
    void collectBuildsCardsFromSitemapAndDetailMetadata() {
        BlogSource source = BlogSource.create(
                "anthropic",
                "Anthropic",
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
                  </head>
                </html>
                """.formatted(publishedAt);
        SitemapArticleCandidateCollector collector = new SitemapArticleCandidateCollector(new StubClient(Map.of(
                "https://www.anthropic.com/sitemap.xml", sitemap,
                "https://www.anthropic.com/engineering/building-effective-agents", detail
        )));

        var cards = collector.collect(source);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().originalTitle()).isEqualTo("Building Effective AI Agents");
        assertThat(cards.getFirst().originalUrl()).isEqualTo("https://www.anthropic.com/engineering/building-effective-agents");
        assertThat(cards.getFirst().publishedAt()).isNotNull();
        assertThat(cards.getFirst().shortContext()).contains("reliable agents");
    }

    private static class StubClient extends SourceDocumentClient {
        private final Map<String, String> documents;

        StubClient(Map<String, String> documents) {
            this.documents = documents;
        }

        @Override
        public String fetch(String url) {
            return documents.get(url);
        }
    }
}
