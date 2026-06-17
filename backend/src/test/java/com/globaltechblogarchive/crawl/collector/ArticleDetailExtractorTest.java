package com.globaltechblogarchive.crawl.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.collector.impl.ArticleDetailExtractor;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
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

    private static class StubClient extends SourceDocumentClient {
        private final String html;

        StubClient(String html) {
            this.html = html;
        }

        @Override
        public String fetch(String url) {
            return html;
        }
    }
}
