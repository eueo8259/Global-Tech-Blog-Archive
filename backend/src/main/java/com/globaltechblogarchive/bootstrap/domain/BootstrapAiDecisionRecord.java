package com.globaltechblogarchive.bootstrap.domain;

import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import java.time.LocalDateTime;

public record BootstrapAiDecisionRecord(
        String recordType,
        String companyKey,
        String articleUrlHash,
        String articleUrl,
        String originalTitle,
        String translatedTitle,
        ArticleCategory category,
        boolean saveTarget,
        String model,
        String promptVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static final String RECORD_TYPE = "AI_DECISION";

    public static BootstrapAiDecisionRecord from(ArticleAiDecision decision) {
        return new BootstrapAiDecisionRecord(
                RECORD_TYPE,
                decision.getCompany().getCompanyKey(),
                decision.getArticleUrlHash(),
                decision.getArticleUrl(),
                decision.getOriginalTitle(),
                decision.getTranslatedTitle(),
                decision.getCategory(),
                decision.isSaveTarget(),
                decision.getModel(),
                decision.getPromptVersion(),
                decision.getCreatedAt(),
                decision.getUpdatedAt()
        );
    }
}
