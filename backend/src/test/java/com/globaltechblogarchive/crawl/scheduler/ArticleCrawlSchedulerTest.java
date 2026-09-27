package com.globaltechblogarchive.crawl.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleCrawlSchedulerTest {

    @Test
    void runScheduledCrawlCallsService() {
        ArticleCrawlService articleCrawlService = mock(ArticleCrawlService.class);
        ArticleCrawlScheduler scheduler = new ArticleCrawlScheduler(articleCrawlService);
        doReturn(result()).when(articleCrawlService).runScheduled();

        scheduler.runScheduledCrawl();

        verify(articleCrawlService).runScheduled();
    }

    @Test
    void runScheduledCrawlSkipsWhenAlreadyRunning() {
        ArticleCrawlService articleCrawlService = mock(ArticleCrawlService.class);
        ArticleCrawlScheduler scheduler = new ArticleCrawlScheduler(articleCrawlService);
        doAnswer(invocation -> {
            scheduler.runScheduledCrawl();
            return result();
        }).when(articleCrawlService).runScheduled();

        scheduler.runScheduledCrawl();

        verify(articleCrawlService, times(1)).runScheduled();
    }

    @Test
    void runScheduledCrawlReleasesRunningFlagAfterFailure() {
        ArticleCrawlService articleCrawlService = mock(ArticleCrawlService.class);
        ArticleCrawlScheduler scheduler = new ArticleCrawlScheduler(articleCrawlService);
        doThrow(new IllegalStateException("crawl failed"))
                .doReturn(result())
                .when(articleCrawlService)
                .runScheduled();

        assertDoesNotThrow(scheduler::runScheduledCrawl);
        scheduler.runScheduledCrawl();

        verify(articleCrawlService, times(2)).runScheduled();
    }

    private ArticleCrawlResult result() {
        return new ArticleCrawlResult(
                1L,
                17,
                17,
                0,
                20,
                3,
                2,
                20,
                2,
                15,
                0,
                0,
                1,
                2,
                List.of()
        );
    }
}
