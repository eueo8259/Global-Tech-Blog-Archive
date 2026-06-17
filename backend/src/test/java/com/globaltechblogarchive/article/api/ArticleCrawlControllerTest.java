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
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
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
                        List.of(candidate),
                        new CrawlRunSummary(
                                1,
                                0,
                                1,
                                1,
                                0,
                                0,
                                0,
                                0
                        )

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

    @Test
    void runInitialReturnsSummaryWithoutCandidateDetails() throws Exception {
        ArticleCrawlResult result = new ArticleCrawlResult(
                2L,
                1,
                1,
                0,
                2,
                0,
                2,
                2,
                1,
                0,
                0,
                1,
                0,
                List.of(SourceCrawlResult.success(
                        com.globaltechblogarchive.source.domain.BlogSource.create(
                                Company.create("openai", "OpenAI"),
                                "openai",
                                "OpenAI News",
                                "https://openai.com/news/",
                                "https://openai.com/news/rss.xml",
                                com.globaltechblogarchive.source.domain.CollectionMethod.RSS
                        ),
                        List.of(),
                        new CrawlRunSummary(2, 0, 2, 1, 0, 0, 1, 0)
                ))
        );
        when(articleCrawlService.runInitial()).thenReturn(result);

        mockMvc.perform(post("/api/admin/article-crawls/initial-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(2))
                .andExpect(jsonPath("$.storedCount").value(2))
                .andExpect(jsonPath("$.aiApprovedCount").value(1))
                .andExpect(jsonPath("$.previouslyApprovedCount").value(1))
                .andExpect(jsonPath("$.sources[0].sourceKey").value("openai"))
                .andExpect(jsonPath("$.sources[0].storedCount").value(2))
                .andExpect(jsonPath("$.sources[0].candidates").doesNotExist());

        verify(articleCrawlService).runInitial();
    }

    @Test
    void runSourceReturnsSingleSourceCrawlResult() throws Exception {
        ArticleCrawlResult result = new ArticleCrawlResult(
                3L,
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
                                Company.create("uber", "Uber"),
                                "uber",
                                "Uber Engineering Blog",
                                "https://www.uber.com/blog/engineering",
                                null,
                                com.globaltechblogarchive.source.domain.CollectionMethod.HTML_SCRAPING
                        ),
                        List.of(),
                        new CrawlRunSummary(1, 0, 1, 1, 0, 0, 0, 0)
                ))
        );
        when(articleCrawlService.runSource("uber")).thenReturn(result);

        mockMvc.perform(post("/api/admin/article-crawls/sources/uber/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(3))
                .andExpect(jsonPath("$.sourceCount").value(1))
                .andExpect(jsonPath("$.sources[0].sourceKey").value("uber"));

        verify(articleCrawlService).runSource("uber");
    }

    @Test
    void runSourceInitialReturnsSingleSourceInitialSummary() throws Exception {
        ArticleCandidate firstCandidate = new ArticleCandidate(
                "uber",
                "Uber",
                "Scaling traffic",
                "https://www.uber.com/kr/en/blog/scaling-real-time-traffic/",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                "Traffic context",
                "hash-1",
                false,
                ArticleCandidateDecisionStatus.AI_APPROVED,
                List.of()
        );
        ArticleCandidate secondCandidate = new ArticleCandidate(
                "uber",
                "Uber",
                "JUnit migration",
                "https://www.uber.com/kr/en/blog/junit-migration/",
                LocalDateTime.of(2026, 5, 1, 0, 0),
                "JUnit context",
                "hash-2",
                false,
                ArticleCandidateDecisionStatus.AI_APPROVED,
                List.of()
        );
        ArticleCrawlResult result = new ArticleCrawlResult(
                4L,
                1,
                1,
                0,
                2,
                0,
                2,
                2,
                2,
                0,
                0,
                0,
                0,
                List.of(SourceCrawlResult.success(
                        com.globaltechblogarchive.source.domain.BlogSource.create(
                                Company.create("uber", "Uber"),
                                "uber",
                                "Uber Engineering Blog",
                                "https://www.uber.com/blog/engineering",
                                null,
                                com.globaltechblogarchive.source.domain.CollectionMethod.HTML_SCRAPING
                        ),
                        List.of(firstCandidate, secondCandidate),
                        new CrawlRunSummary(2, 0, 2, 2, 0, 0, 0, 0)
                ))
        );
        when(articleCrawlService.runSourceInitial("uber")).thenReturn(result);

        mockMvc.perform(post("/api/admin/article-crawls/sources/uber/initial-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(4))
                .andExpect(jsonPath("$.sourceCount").value(1))
                .andExpect(jsonPath("$.sources[0].sourceKey").value("uber"))
                .andExpect(jsonPath("$.sources[0].candidateCount").value(2))
                .andExpect(jsonPath("$.sources[0].candidates").doesNotExist());

        verify(articleCrawlService).runSourceInitial("uber");
    }
}
