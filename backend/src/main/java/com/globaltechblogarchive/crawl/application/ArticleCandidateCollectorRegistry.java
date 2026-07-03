package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.exception.SourceCollectionException;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleCandidateCollectorRegistry {

    private final List<ArticleCandidateCollector> collectors;

    public ArticleCandidateCollector find(CollectionMethod collectionMethod) {
        return collectors.stream()
                .filter(collector -> collector.supports(collectionMethod))
                .findFirst()
                .orElseThrow(() -> new SourceCollectionException(
                        ErrorCode.SOURCE_COLLECTION_CONFIGURATION_ERROR,
                        "Unsupported collection method: " + collectionMethod
                ));
    }
}
