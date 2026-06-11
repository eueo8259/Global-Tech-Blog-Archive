package com.globaltechblogarchive.crawl.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.helper.ArticleListParserPropertiesFixture;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("real")
class RealHtmlArticleListParserTest {

    private final SourceDocumentClient fetcher = new SourceDocumentClient();
    private final HtmlArticleListParser parser = new HtmlArticleListParser(ArticleListParserPropertiesFixture.full());


    @Test
    void parseClaudeBlogReturnsArticleCards() {
        BlogSource source = BlogSource.create(
                Company.create("anthropic", "Anthropic"),
                "claude-blog",
                "Claude Blog",
                "https://claude.com/blog",
                null,
                CollectionMethod.HTML_SCRAPING
        );

        var cards = parser.parse(source, fetcher.fetch(source.getSiteUrl()));

        assertThat(cards).isNotEmpty();
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.originalTitle()).isNotBlank();
            assertThat(card.originalUrl()).startsWith("http");
            assertThat(card.shortContext()).isNotBlank();
        });
    }
}
