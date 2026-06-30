package com.globaltechblogarchive.article.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.api.ArticleCrawlController;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    void runScheduledReturnsSummaryWithoutCandidateDetails() throws Exception {
        ArticleCrawlResult result = result(
                2L,
                source("openai", "OpenAI News", "https://openai.com/news/", "https://openai.com/news/rss.xml", CollectionMethod.RSS),
                new CrawlRunSummary(2, 0, 2, 1, 0, 0, 1, 0)
        );
        when(articleCrawlService.runScheduled()).thenReturn(result);

        mockMvc.perform(post("/api/admin/article-crawls/scheduled-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(2))
                .andExpect(jsonPath("$.storedCount").value(2))
                .andExpect(jsonPath("$.aiApprovedCount").value(1))
                .andExpect(jsonPath("$.previouslyApprovedCount").value(1))
                .andExpect(jsonPath("$.sources[0].sourceKey").value("openai"))
                .andExpect(jsonPath("$.sources[0].storedCount").value(2))
                .andExpect(jsonPath("$.sources[0].candidates").doesNotExist());

        verify(articleCrawlService).runScheduled();
    }

    @Test
    void runSourceBackfillReturnsSingleSourceSummary() throws Exception {
        ArticleCrawlResult result = result(
                4L,
                source("uber", "Uber Engineering Blog", "https://www.uber.com/blog/engineering", null,
                        CollectionMethod.HTML_SCRAPING),
                new CrawlRunSummary(2, 0, 2, 2, 0, 0, 0, 0)
        );
        when(articleCrawlService.runSourceBackfill("uber")).thenReturn(result);

        mockMvc.perform(post("/api/admin/article-crawls/sources/uber/backfill-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(4))
                .andExpect(jsonPath("$.sourceCount").value(1))
                .andExpect(jsonPath("$.sources[0].sourceKey").value("uber"))
                .andExpect(jsonPath("$.sources[0].candidateCount").value(2))
                .andExpect(jsonPath("$.sources[0].candidates").doesNotExist());

        verify(articleCrawlService).runSourceBackfill("uber");
    }

    @Test
    void retryAiFailuresUsesRequestedLimit() throws Exception {
        ArticleCrawlResult result = result(
                5L,
                source("openai", "OpenAI News", "https://openai.com/news/", "https://openai.com/news/rss.xml",
                        CollectionMethod.RSS),
                new CrawlRunSummary(1, 0, 0, 0, 1, 0, 0, 0)
        );
        when(articleCrawlService.retryAiFailures(10)).thenReturn(result);

        mockMvc.perform(post("/api/admin/article-crawls/ai-failures/retry")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(5))
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.aiRejectedCount").value(1));

        verify(articleCrawlService).retryAiFailures(10);
    }

    private ArticleCrawlResult result(Long runId, BlogSource source, CrawlRunSummary summary) {
        return new ArticleCrawlResult(
                runId,
                1,
                1,
                0,
                summary.discoveredCount(),
                summary.duplicateCount(),
                summary.storedCount(),
                summary.candidateCount(),
                summary.aiApprovedCount(),
                summary.aiRejectedCount(),
                summary.aiFailedCount(),
                summary.previouslyApprovedCount(),
                summary.previouslyRejectedCount(),
                List.of(SourceCrawlResult.success(source, candidates(source, summary.candidateCount()), summary))
        );
    }

    private List<ArticleCandidate> candidates(BlogSource source, int count) {
        List<ArticleCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            candidates.add(new ArticleCandidate(
                    source.getSourceKey(),
                    source.getCompany().getCompanyName(),
                    "Article " + index,
                    source.getSiteUrl() + "/article-" + index,
                    LocalDateTime.of(2026, 6, 1, 0, 0),
                    "Context " + index,
                    "hash-" + index,
                    false,
                    ArticleCandidateDecisionStatus.NEW,
                    List.of()
            ));
        }
        return candidates;
    }

    private BlogSource source(
            String sourceKey,
            String sourceName,
            String siteUrl,
            String feedUrl,
            CollectionMethod method
    ) {
        return BlogSource.create(
                Company.create(sourceKey, sourceName),
                sourceKey,
                sourceName,
                siteUrl,
                feedUrl,
                method
        );
    }
}
