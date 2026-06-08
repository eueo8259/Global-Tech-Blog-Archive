package com.globaltechblogarchive.crawl.parser;

import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.List;

public interface ArticleListParser {

    boolean supports(BlogSource source);

    List<ParsedArticleCard> parse(BlogSource source, String html);
}

