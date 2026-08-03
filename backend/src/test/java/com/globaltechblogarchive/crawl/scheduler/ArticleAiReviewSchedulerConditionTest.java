package com.globaltechblogarchive.crawl.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.globaltechblogarchive.crawl.application.ArticleAiReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ArticleAiReviewSchedulerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void aiReviewWorkerIsEnabledWhenCollectionSchedulerIsDisabled() {
        contextRunner
                .withPropertyValues("crawl.scheduler.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(ArticleAiReviewScheduler.class));
    }

    @Test
    void aiReviewWorkerCanBeDisabledIndependently() {
        contextRunner
                .withPropertyValues(
                        "crawl.scheduler.enabled=true",
                        "crawl.ai-review.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(ArticleAiReviewScheduler.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ArticleAiReviewScheduler.class)
    static class TestConfiguration {

        @Bean
        ArticleAiReviewService articleAiReviewService() {
            return mock(ArticleAiReviewService.class);
        }
    }
}
