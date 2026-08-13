package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.application.ArticleAiReviewService;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.CrawlTransactionService;
import com.globaltechblogarchive.crawl.application.SourceCrawlProcessor;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleCrawlServiceTest {

    @Mock
    private BlogSourceRepository sourceRepository;

    @Mock
    private SourceCrawlProcessor sourceProcessor;

    @Mock
    private CrawlTransactionService transactionService;

    @Mock
    private ArticleAiReviewService aiReviewService;

    private ArticleCrawlService crawlService;

    @BeforeEach
    void setUp() {
        crawlService = new ArticleCrawlService(
                sourceRepository,
                sourceProcessor,
                transactionService,
                aiReviewService,
                new AiReviewProperties(
                        50,
                        10,
                        3,
                        100,
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(30)
                )
        );
    }

    @Test
    void scheduledCollectionContinuesWhenOneSourceFails() {
        BlogSource failing = source(1L, "failing");
        BlogSource succeeding = source(2L, "succeeding");
        when(transactionService.startRun()).thenReturn(10L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(failing, succeeding));
        when(sourceProcessor.process(10L, 1L, CrawlPolicy.recent()))
                .thenThrow(new IllegalStateException("network failed"));
        when(sourceProcessor.process(10L, 2L, CrawlPolicy.recent()))
                .thenReturn(SourceCrawlResult.success(succeeding, List.of(), CrawlRunSummary.empty()));

        ArticleCrawlResult result = crawlService.runScheduled();

        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        verify(transactionService).markSourceFailed(1L, "network failed");
        verify(transactionService).completeRun(10L, 2, 1, 1, CrawlRunSummary.empty());
    }

    @Test
    void sourceBackfillUsesOnlyRequestedSource() {
        BlogSource source = source(3L, "uber");
        when(transactionService.startRun()).thenReturn(11L);
        when(sourceRepository.findBySourceKeyAndEnabledTrue("uber")).thenReturn(Optional.of(source));
        when(sourceProcessor.process(11L, 3L, CrawlPolicy.backfill(30)))
                .thenReturn(SourceCrawlResult.success(source, List.of(), CrawlRunSummary.empty()));

        ArticleCrawlResult result = crawlService.runSourceBackfill("uber", 30);

        assertThat(result.runId()).isEqualTo(11L);
        verify(sourceRepository, never()).findByEnabledTrue();
        verify(sourceProcessor).process(11L, 3L, CrawlPolicy.backfill(30));
    }

    @Test
    void allBackfillUsesEveryEnabledSourceAndRequestedLimit() {
        BlogSource first = source(4L, "first");
        BlogSource second = source(5L, "second");
        when(transactionService.startRun()).thenReturn(12L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(first, second));
        when(sourceProcessor.process(12L, 4L, CrawlPolicy.backfill(40)))
                .thenReturn(SourceCrawlResult.success(first, List.of(), CrawlRunSummary.empty()));
        when(sourceProcessor.process(12L, 5L, CrawlPolicy.backfill(40)))
                .thenReturn(SourceCrawlResult.success(second, List.of(), CrawlRunSummary.empty()));

        ArticleCrawlResult result = crawlService.runAllBackfill(40);

        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        verify(sourceProcessor).process(12L, 4L, CrawlPolicy.backfill(40));
        verify(sourceProcessor).process(12L, 5L, CrawlPolicy.backfill(40));
    }

    @Test
    void sourceBackfillRejectsUnknownSource() {
        when(sourceRepository.findBySourceKeyAndEnabledTrue("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crawlService.runSourceBackfill("missing", 50))
                .isInstanceOfSatisfying(InvalidInputException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

        verify(transactionService, never()).startRun();
    }

    @Test
    void backfillRejectsLimitOutsideAllowedRange() {
        assertThatThrownBy(() -> crawlService.runAllBackfill(51))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Backfill max candidates per source must be between 1 and 50");

        verify(sourceRepository, never()).findByEnabledTrue();
        verify(transactionService, never()).startRun();
    }

    @Test
    void retryAiFailuresReturnsAiResultWithoutCreatingCollectionRun() {
        when(aiReviewService.retryFailed(20)).thenReturn(
                new AiReviewRunResult(3, 1, 0, 1, 1, 1, 0)
        );

        ArticleCrawlResult result = crawlService.retryAiFailures(20);

        assertThat(result.runId()).isNull();
        assertThat(result.candidateCount()).isEqualTo(3);
        assertThat(result.aiApprovedCount()).isEqualTo(1);
        assertThat(result.aiRejectedCount()).isZero();
        assertThat(result.aiFailedCount()).isEqualTo(1);
        assertThat(result.aiRetryWaitingCount()).isEqualTo(1);
        assertThat(result.storedCount()).isEqualTo(1);
        assertThat(result.sources()).isEmpty();
        verify(transactionService, never()).startRun();
    }

    @Test
    void retryAiFailuresDoesNotLeaveCollectionRunWhenReviewAborts() {
        when(aiReviewService.retryFailed(20)).thenThrow(new IllegalStateException("claim failed"));

        assertThatThrownBy(() -> crawlService.retryAiFailures(20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("claim failed");

        verify(transactionService, never()).startRun();
    }

    @Test
    void retryAiFailuresRejectsLimitOutsideAllowedRange() {
        assertThatThrownBy(() -> crawlService.retryAiFailures(0))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("AI failure retry limit must be between 1 and 100");

        verify(aiReviewService, never()).retryFailed(0);
        verify(transactionService, never()).startRun();
    }

    private BlogSource source(Long id, String key) {
        Company company = Company.create(key, key);
        ReflectionTestUtils.setField(company, "id", id);
        BlogSource source = BlogSource.create(
                company,
                key,
                key,
                "https://example.com",
                "https://example.com/feed",
                CollectionMethod.RSS
        );
        ReflectionTestUtils.setField(source, "id", id);
        return source;
    }
}
