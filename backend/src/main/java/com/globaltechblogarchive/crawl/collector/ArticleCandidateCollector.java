package com.globaltechblogarchive.crawl.collector;

import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;

public interface ArticleCandidateCollector {

    boolean supports(CollectionMethod collectionMethod);

    List<ParsedArticle> collect(BlogSource source, CrawlPolicy policy);
}
