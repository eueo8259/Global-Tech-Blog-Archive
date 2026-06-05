package com.globaltechblogarchive.article.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.collection.application.ArticleCrawlResult;
import com.globaltechblogarchive.collection.application.ArticleCrawlService;
import com.globaltechblogarchive.collection.application.SourceCrawlResult;
import com.globaltechblogarchive.collection.domain.ArticleCandidate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ArticleCrawlControllerTest {

    private ArticleCrawlService articleCrawlService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        articleCrawlService = mock(ArticleCrawlService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ArticleCrawlController(articleCrawlService)).build();
    }

    @Test
    void runReturnsCrawlResultStructure() throws Exception {
        ArticleCandidate candidate = new ArticleCandidate(
                "stripe",
                "Stripe",
                "Scaling APIs",
                "https://stripe.com/blog/scaling-apis",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                "API context",
                "https://stripe.com/blog/scaling-apis",
                "hash",
                false,
                List.of()
        );
        ArticleCrawlResult result = new ArticleCrawlResult(
                1L,
                1,
                1,
                0,
                1,
                0,
                1,
                1,
                List.of(SourceCrawlResult.success(
                        com.globaltechblogarchive.source.domain.BlogSource.create(
                                "stripe",
                                "Stripe",
                                "https://stripe.com/blog/engineering",
                                null,
                                com.globaltechblogarchive.source.domain.CollectionMethod.HTML_SCRAPING
                        ),
                        List.of(candidate)
                ))
        );
        when(articleCrawlService.run()).thenReturn(result);

        mockMvc.perform(post("/api/admin/article-crawls/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(1))
                .andExpect(jsonPath("$.sourceCount").value(1))
                .andExpect(jsonPath("$.storedCount").value(1))
                .andExpect(jsonPath("$.sources[0].companyKey").value("stripe"))
                .andExpect(jsonPath("$.sources[0].candidates[0].originalTitle").value("Scaling APIs"))
                .andExpect(jsonPath("$.sources[0].candidates[0].duplicate").value(false))
                .andExpect(jsonPath("$.sources[0].candidates[0].validationWarnings").isArray())
                .andExpect(jsonPath("$.sources[0].qualityWarnings").isArray());

        verify(articleCrawlService).run();
    }
}
