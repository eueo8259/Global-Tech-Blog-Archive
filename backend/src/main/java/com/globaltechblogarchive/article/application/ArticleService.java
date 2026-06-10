package com.globaltechblogarchive.article.application;

import com.globaltechblogarchive.article.api.dto.ArticlePageResponse;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Transactional
    public void save(Article article) {
        articleRepository.save(article);
    }


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

    //TODO: ArticleCategory.valueOf(category) Enum으로 로직 이동하기 (문자열을 도메인 값으로 바꾸는 로직으로 ArticleCategory 에 있는게 더 자연스러움
    private ArticleCategory parseArticleCategory(String category) {
        if (category == null) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Unsupported category: null");
        }

        try {
            return ArticleCategory.valueOf(category);
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Unsupported category: " + category);
        }
    }
}
