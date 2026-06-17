package com.globaltechblogarchive.crawl.collector.impl;

import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.parser.ArticleListParser;
import com.globaltechblogarchive.crawl.parser.ArticleListParserRegistry;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.support.ArticleCandidateCollectionPolicy;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
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
    private final ArticleDetailExtractor detailExtractor;

    @Override
    public boolean supports(CollectionMethod collectionMethod) {
        return collectionMethod == CollectionMethod.HTML_SCRAPING;
    }

    @Override
    public List<ParsedArticle> collect(BlogSource source, CrawlMode mode) {
        ArticleListParser parser = parserRegistry.find(source);
        List<ParsedArticle> parsedArticles = collectListPages(source, parser, mode);
        List<ParsedArticle> detailTargets = ArticleCandidateCollectionPolicy.apply(parsedArticles, mode);
        List<ParsedArticle> detailedArticles = detailTargets.stream()
                .map(detailExtractor::extract)
                .toList();
        return ArticleCandidateCollectionPolicy.apply(detailedArticles, mode);
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
}
