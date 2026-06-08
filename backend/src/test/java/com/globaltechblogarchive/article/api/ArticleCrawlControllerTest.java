package com.globaltechblogarchive.article.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.crawl.api.ArticleCrawlController;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.company.domain.Company;
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
                ArticleCandidateDecisionStatus.NEW,
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
                1,
                0,
                0,
                0,
                0,
                List.of(SourceCrawlResult.success(
                        com.globaltechblogarchive.source.domain.BlogSource.create(
                                Company.create("stripe", "Stripe"),
                                "stripe",
                                "Stripe Engineering",
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
                .andExpect(jsonPath("$.aiApprovedCount").value(1))
                .andExpect(jsonPath("$.aiRejectedCount").value(0))
                .andExpect(jsonPath("$.aiFailedCount").value(0))
                .andExpect(jsonPath("$.previouslyApprovedCount").value(0))
                .andExpect(jsonPath("$.previouslyRejectedCount").value(0))
                .andExpect(jsonPath("$.sources[0].companyKey").value("stripe"))
                .andExpect(jsonPath("$.sources[0].candidates[0].originalTitle").value("Scaling APIs"))
                .andExpect(jsonPath("$.sources[0].candidates[0].duplicate").value(false))
                .andExpect(jsonPath("$.sources[0].candidates[0].decisionStatus").value("NEW"))
                .andExpect(jsonPath("$.sources[0].candidates[0].validationWarnings").isArray())
                .andExpect(jsonPath("$.sources[0].qualityWarnings").isArray());

        verify(articleCrawlService).run();
    }
}
