package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.application.ArticleAiReviewResultService;
import com.globaltechblogarchive.crawl.application.ArticleAiReviewService;
import com.globaltechblogarchive.crawl.application.ArticleCandidateStateService;
import com.globaltechblogarchive.crawl.application.SourceCrawlProcessor;
import com.globaltechblogarchive.crawl.application.dto.ClaimedArticleCandidate;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class CrawlTransactionBoundaryTest {

    @Test
    void externalCallOrchestratorsAreNotTransactional() throws NoSuchMethodException {
        assertThat(SourceCrawlProcessor.class.getMethod(
                "process",
                Long.class,
                Long.class,
                CrawlPolicy.class
        ).getAnnotation(Transactional.class)).isNull();
        assertThat(ArticleAiReviewService.class.getMethod("runScheduled")
                .getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void claimAndResultWritesHaveShortTransactions() throws NoSuchMethodException {
        assertThat(ArticleCandidateStateService.class.getMethod(
                "claimAvailable",
                LocalDateTime.class,
                String.class,
                int.class
        ).getAnnotation(Transactional.class)).isNotNull();
        assertThat(ArticleAiReviewResultService.class.getMethod(
                "complete",
                ClaimedArticleCandidate.class,
                com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision.class,
                String.class
        ).getAnnotation(Transactional.class)).isNotNull();
    }
}
