package com.globaltechblogarchive.crawl.collector.impl;

import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.parser.ArticleListParser;
import com.globaltechblogarchive.crawl.parser.ArticleListParserRegistry;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.support.ArticleCandidateCollectionPolicy;
import com.globaltechblogarchive.crawl.support.ArticleDateParser;
import com.globaltechblogarchive.crawl.support.HtmlMetadataExtractor;
import com.globaltechblogarchive.crawl.support.TextCleaner;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HtmlArticleCandidateCollector implements ArticleCandidateCollector {

    private final SourceDocumentClient fetcher;
    private final ArticleListParserRegistry parserRegistry;

    @Override
    public boolean supports(CollectionMethod collectionMethod) {
        return collectionMethod == CollectionMethod.HTML_SCRAPING;
    }

    @Override
    public List<ParsedArticle> collect(BlogSource source, CrawlMode mode) {
        ArticleListParser parser = parserRegistry.find(source);
        List<ParsedArticle> parsedArticles = collectListPages(source, parser, mode);
        if ("discord".equals(source.getSourceKey())) {
            return collectDiscordDetails(parsedArticles, mode);
        }
        return ArticleCandidateCollectionPolicy.apply(parsedArticles, mode);
    }

    private List<ParsedArticle> collectListPages(BlogSource source, ArticleListParser parser, CrawlMode mode) {
        if ("uber".equals(source.getSourceKey()) && mode == CrawlMode.BACKFILL) {
            Map<String, ParsedArticle> articles = new LinkedHashMap<>();
            for (String url : List.of(source.getSiteUrl(), pageUrl(source.getSiteUrl(), 2))) {
                for (ParsedArticle article : parser.parse(source, fetcher.fetch(url))) {
                    articles.putIfAbsent(article.originalUrl(), article);
                }
            }
            return List.copyOf(articles.values());
        }
        return parser.parse(source, fetcher.fetch(source.getSiteUrl()));
    }

    private String pageUrl(String siteUrl, int page) {
        String baseUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
        return baseUrl + "/page/" + page;
    }

    private List<ParsedArticle> collectDiscordDetails(List<ParsedArticle> parsedArticles, CrawlMode mode) {
        List<ParsedArticle> detailedArticles = parsedArticles.stream()
                .limit(ArticleCandidateCollectionPolicy.maxCandidates(mode))
                .map(this::fetchDiscordDetail)
                .toList();
        return ArticleCandidateCollectionPolicy.apply(detailedArticles, mode);
    }

    private ParsedArticle fetchDiscordDetail(ParsedArticle article) {
        String html = fetcher.fetch(article.originalUrl());
        String title = firstNonBlank(
                HtmlMetadataExtractor.heading(html, 1),
                HtmlMetadataExtractor.jsonLdText(html, "headline"),
                HtmlMetadataExtractor.metaContent(html, "og:title"),
                article.originalTitle()
        );
        LocalDateTime publishedAt = firstNonNull(
                ArticleDateParser.parseListPageDate(HtmlMetadataExtractor.firstDateAfterHeading(html, 1)),
                ArticleDateParser.parseListPageDate(HtmlMetadataExtractor.jsonLdText(html, "datePublished")),
                article.publishedAt()
        );
        String description = firstNonBlank(
                HtmlMetadataExtractor.metaContent(html, "description"),
                HtmlMetadataExtractor.jsonLdText(html, "description"),
                article.shortContext()
        );
        return new ParsedArticle(
                TextCleaner.clean(title),
                article.originalUrl(),
                publishedAt,
                TextCleaner.shortContext(description, title)
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
