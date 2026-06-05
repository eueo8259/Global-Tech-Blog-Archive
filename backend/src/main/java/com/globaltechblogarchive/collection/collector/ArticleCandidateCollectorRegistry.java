package com.globaltechblogarchive.collection.collector;

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
                .orElseThrow(() -> new IllegalArgumentException("Unsupported collection method: " + collectionMethod));
    }
}

