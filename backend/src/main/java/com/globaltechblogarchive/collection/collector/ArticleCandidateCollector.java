package com.globaltechblogarchive.collection.collector;

import com.globaltechblogarchive.collection.parser.ParsedArticleCard;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;

public interface ArticleCandidateCollector {

    boolean supports(CollectionMethod collectionMethod);

    List<ParsedArticleCard> collect(BlogSource source);
}
