package com.globaltechblogarchive.article.api.dto;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import java.time.LocalDateTime;

public record ArticleResponse(
        Long id,
        String title,
        String originalUrl,
        ArticleCategory category,
        LocalDateTime publishedAt,
        String companyKey,
        String companyName
) {

    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getOriginalUrl(),
                article.getCategory(),
                article.getPublishedAt(),
                article.getCompany().getCompanyKey(),
                article.getCompany().getCompanyName()
        );
    }
}
