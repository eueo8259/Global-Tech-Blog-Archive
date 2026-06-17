package com.globaltechblogarchive.crawl.collector.impl;

import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.support.ArticleDateParser;
import com.globaltechblogarchive.crawl.support.HtmlMetadataExtractor;
import com.globaltechblogarchive.crawl.support.TextCleaner;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleDetailExtractor {

    private final SourceDocumentClient fetcher;

    public ParsedArticle extract(ParsedArticle article) {
        String html = fetcher.fetch(article.originalUrl());
        String title = firstNonBlank(
                HtmlMetadataExtractor.heading(html, 1),
                HtmlMetadataExtractor.jsonLdText(html, "headline"),
                HtmlMetadataExtractor.metaContent(html, "og:title"),
                article.originalTitle()
        );
        LocalDateTime publishedAt = firstNonNull(
                ArticleDateParser.parseSitemapDate(HtmlMetadataExtractor.metaContent(html, "article:published_time")),
                ArticleDateParser.parseListPageDate(HtmlMetadataExtractor.jsonLdText(html, "datePublished")),
                ArticleDateParser.parseListPageDate(HtmlMetadataExtractor.firstDateAfterHeading(html, 1)),
                article.publishedAt()
        );
        String shortContext = firstNonBlank(
                HtmlMetadataExtractor.metaContent(html, "description"),
                HtmlMetadataExtractor.metaContent(html, "og:description"),
                HtmlMetadataExtractor.jsonLdText(html, "description"),
                HtmlMetadataExtractor.firstParagraph(html),
                article.shortContext(),
                title
        );
        return new ParsedArticle(
                TextCleaner.clean(title),
                article.originalUrl(),
                publishedAt,
                TextCleaner.shortContext(shortContext, title)
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private LocalDateTime firstNonNull(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
