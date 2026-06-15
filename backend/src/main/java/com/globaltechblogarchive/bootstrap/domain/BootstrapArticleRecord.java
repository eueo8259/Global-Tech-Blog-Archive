package com.globaltechblogarchive.bootstrap.domain;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import java.time.LocalDateTime;

public record BootstrapArticleRecord(
        String recordType,
        String companyKey,
        String title,
        String articleUrl,
        String articleUrlHash,
        ArticleCategory category,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static final String RECORD_TYPE = "ARTICLE";

    public static BootstrapArticleRecord from(Article article) {
        return new BootstrapArticleRecord(
                RECORD_TYPE,
                article.getCompany().getCompanyKey(),
                article.getTitle(),
                article.getArticleUrl(),
                article.getArticleUrlHash(),
                article.getCategory(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
