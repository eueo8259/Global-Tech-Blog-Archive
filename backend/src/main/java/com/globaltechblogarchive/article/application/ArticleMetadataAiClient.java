package com.globaltechblogarchive.article.application;

import com.globaltechblogarchive.article.domain.ArticleCategory;
import java.util.List;

public interface ArticleMetadataAiClient {

    String model();

    List<ArticleMetadataDecision> decide(List<ArticleMetadataInput> inputs);

    record ArticleMetadataInput(
            int index,
            String title,
            String shortContext
    ) {
    }

    record ArticleMetadataDecision(
            int index,
            String translatedTitle,
            ArticleCategory category,
            boolean save,
            String exclusionReason
    ) {
    }
}
