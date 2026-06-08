package com.globaltechblogarchive.crawl.parser;

import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleListParserRegistry {

    private final List<ArticleListParser> parsers;

    public ArticleListParser find(BlogSource source) {
        return parsers.stream()
                .filter(parser -> parser.supports(source))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No article list parser for source: "
                        + source.getCompanyKey()));
    }
}

