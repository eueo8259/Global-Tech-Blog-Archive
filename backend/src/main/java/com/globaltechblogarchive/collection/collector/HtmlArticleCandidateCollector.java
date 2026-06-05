package com.globaltechblogarchive.collection.collector;

import com.globaltechblogarchive.collection.parser.ArticleListParser;
import com.globaltechblogarchive.collection.parser.ArticleListParserRegistry;
import com.globaltechblogarchive.collection.parser.ParsedArticleCard;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HtmlArticleCandidateCollector implements ArticleCandidateCollector {

    private final SourceDocumentFetcher fetcher;
    private final ArticleListParserRegistry parserRegistry;

    @Override
    public boolean supports(CollectionMethod collectionMethod) {
        return collectionMethod == CollectionMethod.HTML_SCRAPING;
    }

    @Override
    public List<ParsedArticleCard> collect(BlogSource source) {
        ArticleListParser parser = parserRegistry.find(source);
        return parser.parse(source, fetcher.fetch(source.getSiteUrl()));
    }
}
