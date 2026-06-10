package com.globaltechblogarchive.crawl.collector.impl;

import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.client.SourceDocumentClient;
import com.globaltechblogarchive.crawl.parser.ArticleListParser;
import com.globaltechblogarchive.crawl.parser.ArticleListParserRegistry;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.support.ArticleCandidateCollectionPolicy;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;
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
    public List<ParsedArticle> collect(BlogSource source) {
        ArticleListParser parser = parserRegistry.find(source);
        return ArticleCandidateCollectionPolicy.apply(parser.parse(source, fetcher.fetch(source.getSiteUrl())));
    }
}
