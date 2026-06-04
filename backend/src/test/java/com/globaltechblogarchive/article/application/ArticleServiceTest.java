package com.globaltechblogarchive.article.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.article.api.dto.ArticlePageResponse;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void getArticlesReturnsAllArticlesWithSourceCompanyInfo() {
        Article article = article(1L, ArticleCategory.AI);
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(articleRepository.findAllByOrderByPublishedAtDescIdDesc(pageRequest))
                .thenReturn(new PageImpl<>(List.of(article), pageRequest, 1));

        ArticlePageResponse response = articleService.getArticles("ALL", 0, 20);

        assertThat(response.articles()).hasSize(1);
        assertThat(response.articles().getFirst().sourceCompanyKey()).isEqualTo("openai");
        assertThat(response.articles().getFirst().sourceCompanyName()).isEqualTo("OpenAI");
        assertThat(response.totalElements()).isEqualTo(1);
        verify(articleRepository).findAllByOrderByPublishedAtDescIdDesc(pageRequest);
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getArticlesFiltersByCategory() {
        Article article = article(2L, ArticleCategory.BACKEND);
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(articleRepository.findByCategoryOrderByPublishedAtDescIdDesc(ArticleCategory.BACKEND, pageRequest))
                .thenReturn(new PageImpl<>(List.of(article), pageRequest, 1));

        ArticlePageResponse response = articleService.getArticles("BACKEND", 0, 20);

        assertThat(response.articles()).hasSize(1);
        assertThat(response.articles().getFirst().category()).isEqualTo(ArticleCategory.BACKEND);
        verify(articleRepository).findByCategoryOrderByPublishedAtDescIdDesc(ArticleCategory.BACKEND, pageRequest);
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getArticlesRejectsInvalidCategory() {
        Throwable throwable = catchThrowable(() -> articleService.getArticles("Backend", 0, 20));

        assertThat(throwable).isInstanceOf(InvalidInputException.class);
        InvalidInputException exception = (InvalidInputException) throwable;
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThat(exception).hasMessage("Unsupported category: Backend");

        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getArticlesRejectsNullCategory() {
        assertThatThrownBy(() -> articleService.getArticles(null, 0, 20))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Unsupported category: null");

        verifyNoMoreInteractions(articleRepository);
    }

    private Article article(Long id, ArticleCategory category) {
        BlogSource source = BlogSource.create(
                "openai",
                "OpenAI",
                "https://openai.com/news/",
                "https://openai.com/news/rss.xml",
                CollectionMethod.RSS
        );
        Article article = Article.create(
                source,
                "Article " + id,
                "Summary " + id,
                "https://openai.com/news/article-" + id,
                "https://openai.com/news/article-" + id,
                "hash-" + id,
                category,
                LocalDateTime.of(2026, 6, 1, 10, 0)
        );
        ReflectionTestUtils.setField(article, "id", id);
        return article;
    }
}
