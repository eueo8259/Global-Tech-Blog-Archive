package com.globaltechblogarchive.article.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.article.api.dto.ArticlePageResponse;
import com.globaltechblogarchive.article.api.dto.ArticleResponse;
import com.globaltechblogarchive.article.application.ArticleService;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.GlobalExceptionHandler;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ArticleControllerTest {

    private ArticleService articleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        articleService = mock(ArticleService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ArticleController(articleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getArticlesUsesDefaultQueryValues() throws Exception {
        ArticleResponse article = new ArticleResponse(
                1L,
                "Translated title",
                "https://example.com/article",
                ArticleCategory.AI,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                "openai",
                "OpenAI"
        );
        ArticlePageResponse response = new ArticlePageResponse(List.of(article), 0, 20, 1, 1, false);
        when(articleService.getArticles("ALL", 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.articles").isArray())
                .andExpect(jsonPath("$.articles[0].title").value("Translated title"))
                .andExpect(jsonPath("$.articles[0].summary").doesNotExist());

        verify(articleService).getArticles("ALL", 0, 20);
    }

    @Test
    void getArticlesAcceptsCategoryFilter() throws Exception {
        ArticlePageResponse response = new ArticlePageResponse(List.of(), 1, 10, 0, 0, false);
        when(articleService.getArticles("BACKEND", 1, 10)).thenReturn(response);

        mockMvc.perform(get("/api/articles")
                        .param("category", "BACKEND")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(articleService).getArticles("BACKEND", 1, 10);
    }

    @Test
    void getArticlesRejectsInvalidCategory() throws Exception {
        when(articleService.getArticles("Backend", 0, 20))
                .thenThrow(new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Unsupported category: Backend"));

        mockMvc.perform(get("/api/articles")
                        .param("category", "Backend"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("Unsupported category: Backend"));

        verify(articleService).getArticles("Backend", 0, 20);
    }

    @Test
    void getArticlesRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/articles")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"));
    }

    @Test
    void getArticlesRejectsInvalidPageType() throws Exception {
        mockMvc.perform(get("/api/articles")
                        .param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("page has invalid value"));
    }
}
