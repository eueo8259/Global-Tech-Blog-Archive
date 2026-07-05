package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;

public record PreparedArticleDecision(
        String articleUrlHash,
        String articleUrl,
        String originalTitle,
        String translatedTitle,
        ArticleCategory category,
        boolean saveTarget,
        String model,
        String promptVersion
) {

    public static PreparedArticleDecision from(ArticleAiDecision decision) {
        return new PreparedArticleDecision(
                decision.getArticleUrlHash(),
                decision.getArticleUrl(),
                decision.getOriginalTitle(),
                decision.getTranslatedTitle(),
                decision.getCategory(),
                decision.isSaveTarget(),
                decision.getModel(),
                decision.getPromptVersion()
        );
    }
}
