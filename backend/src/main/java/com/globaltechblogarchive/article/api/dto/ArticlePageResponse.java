package com.globaltechblogarchive.article.api.dto;

import com.globaltechblogarchive.article.domain.Article;
import java.util.List;
import org.springframework.data.domain.Page;

public record ArticlePageResponse(
        List<ArticleResponse> articles,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static ArticlePageResponse from(Page<Article> page) {
        return new ArticlePageResponse(
                page.getContent().stream()
                        .map(ArticleResponse::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
