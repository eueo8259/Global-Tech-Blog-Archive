package com.globaltechblogarchive.crawl.collector;

import com.globaltechblogarchive.crawl.parser.ParsedArticleCard;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;

public interface ArticleCandidateCollector {

    boolean supports(CollectionMethod collectionMethod);

    List<ParsedArticleCard> collect(BlogSource source);
}
