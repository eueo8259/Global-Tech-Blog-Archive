package com.globaltechblogarchive.article.application;

import com.globaltechblogarchive.article.api.dto.ArticlePageResponse;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public ArticlePageResponse getArticles(String category, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Article> articles;

        if ("ALL".equals(category)) {
            articles = articleRepository.findAllByOrderByPublishedAtDescIdDesc(pageRequest);
            return ArticlePageResponse.from(articles);
        }

        ArticleCategory articleCategory = parseArticleCategory(category);
        articles = articleRepository.findByCategoryOrderByPublishedAtDescIdDesc(articleCategory, pageRequest);
        return ArticlePageResponse.from(articles);
    }

    private ArticleCategory parseArticleCategory(String category) {
        if (category == null) {
            throw new InvalidInputException("Unsupported category: null");
        }

        try {
            return ArticleCategory.valueOf(category);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException("Unsupported category: " + category);
        }
    }
}
