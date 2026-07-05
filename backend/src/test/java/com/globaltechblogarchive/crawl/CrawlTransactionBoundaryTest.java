package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.application.CrawlPersistenceService;
import com.globaltechblogarchive.crawl.application.SourceCrawlProcessor;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class CrawlTransactionBoundaryTest {

    @Test
    void externalCallOrchestratorMethodsAreNotTransactional() throws NoSuchMethodException {
        assertThat(SourceCrawlProcessor.class.getMethod(
                "process",
                Long.class,
                Long.class,
                CrawlMode.class
        ).getAnnotation(Transactional.class)).isNull();
        assertThat(SourceCrawlProcessor.class.getMethod(
                "retryAiFailures",
                Long.class,
                Long.class,
                List.class
        ).getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void persistenceMethodsAreTransactional() throws NoSuchMethodException {
        assertThat(CrawlPersistenceService.class.getMethod(
                "persistCollection",
                Long.class,
                Long.class,
                com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates.class
        ).getAnnotation(Transactional.class)).isNotNull();
        assertThat(CrawlPersistenceService.class.getMethod(
                "persistRetry",
                Long.class,
                Long.class,
                com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates.class
        ).getAnnotation(Transactional.class)).isNotNull();
    }
}
