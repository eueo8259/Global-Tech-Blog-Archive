package com.globaltechblogarchive.crawl.parser;

import com.globaltechblogarchive.crawl.exception.SourceCollectionException;
import com.globaltechblogarchive.global.error.ErrorCode;
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
                .orElseThrow(() -> new SourceCollectionException(
                        ErrorCode.SOURCE_COLLECTION_CONFIGURATION_ERROR,
                        "No article list parser for source: " + source.getSourceKey()
                ));
    }
}

