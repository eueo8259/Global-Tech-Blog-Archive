package com.globaltechblogarchive.crawl.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.collector.impl.ArticleDetailExtractor;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ArticleDetailExtractorTest {

    @Test
    void extractFallsBackToFirstParagraphWithoutScriptStyleOrSvgText() {
        String articleUrl = "https://example.com/blog/detail";
        SourceDocumentClient client = new StubClient("""
                <html>
                  <head>
                    <style>.article { color: red; }</style>
                    <script>{"description":"script text should not win"}</script>
                  </head>
                  <body>
                    <svg><text>svg text should not win</text></svg>
                    <h1>Detail title</h1>
                    <p>First visible paragraph for classification.</p>
                  </body>
                </html>
                """);
        ArticleDetailExtractor extractor = new ArticleDetailExtractor(client);

        ParsedArticle article = extractor.extract(new ParsedArticle(
                "Listing title",
                articleUrl,
                LocalDateTime.of(2026, 6, 16, 0, 0),
                "Listing context"
        ));

        assertThat(article.originalTitle()).isEqualTo("Detail title");
        assertThat(article.shortContext()).isEqualTo("First visible paragraph for classification.");
    }

    @Test
    void extractUsesVisibleMetadataDateAfterHeading() {
        String articleUrl = "https://stripe.dev/blog/modern-java-at-stripe-language-upgrades-as-a-service";
        SourceDocumentClient client = new StubClient("""
                <html>
                  <body>
                    <h1>Modern Java at Stripe: Language upgrades as a service</h1>
                    <section>
                      <h2>Metadata</h2>
                      <div>Date:2026.5.27</div>
                      <div>Reading time:6 min read</div>
                    </section>
                    <p>How Stripe upgrades Java across a large JVM codebase.</p>
                  </body>
                </html>
                """);
        ArticleDetailExtractor extractor = new ArticleDetailExtractor(client);

        ParsedArticle article = extractor.extract(new ParsedArticle(
                "Listing title",
                articleUrl,
                null,
                "Listing context"
        ));

        assertThat(article.publishedAt()).isEqualTo(LocalDateTime.of(2026, 5, 27, 0, 0));
    }

    private static class StubClient extends SourceDocumentClient {
        private final String html;

        StubClient(String html) {
            super(new SimpleMeterRegistry());
            this.html = html;
        }

        @Override
        public String fetch(String sourceKey, String url) {
            return html;
        }
    }
}
