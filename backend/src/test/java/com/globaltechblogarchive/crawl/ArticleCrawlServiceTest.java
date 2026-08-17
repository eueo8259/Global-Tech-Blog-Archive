package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.application.ArticleAiReviewService;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.CrawlTransactionService;
import com.globaltechblogarchive.crawl.application.CrawlPersistenceService;
import com.globaltechblogarchive.crawl.application.CrawlPipelineMetrics;
import com.globaltechblogarchive.crawl.application.SourceCrawlProcessor;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.PreparedSourceCrawl;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.config.CrawlExecutionConfig;
import com.globaltechblogarchive.crawl.config.CrawlExecutionProperties;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class ArticleCrawlServiceTest {

    @Mock
    private BlogSourceRepository sourceRepository;

    @Mock
    private SourceCrawlProcessor sourceProcessor;

    @Mock
    private CrawlPersistenceService persistenceService;

    @Mock
    private CrawlTransactionService transactionService;

    @Mock
    private ArticleAiReviewService aiReviewService;

    private ArticleCrawlService crawlService;
    private ThreadPoolTaskExecutor crawlSourceExecutor;
    private final Map<Long, BlogSource> sourcesById = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        crawlSourceExecutor = new CrawlExecutionConfig().crawlSourceExecutor(
                new CrawlExecutionProperties(2)
        );
        crawlSourceExecutor.initialize();
        lenient().when(persistenceService.persistDiscoveredCandidates(
                        anyLong(), anyLong(), any(), any()
                ))
                .thenAnswer(invocation -> {
                    Long sourceId = invocation.getArgument(1);
                    List<ArticleCandidate> candidates = invocation.getArgument(2);
                    return SourceCrawlResult.success(
                            sourcesById.get(sourceId),
                            candidates,
                            CrawlRunSummary.empty()
                    );
                });
        crawlService = new ArticleCrawlService(
                sourceRepository,
                sourceProcessor,
                persistenceService,
                transactionService,
                aiReviewService,
                new AiReviewProperties(
                        50,
                        10,
                        3,
                        100,
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(30)
                ),
                crawlSourceExecutor,
                new Semaphore(1, true),
                new CrawlPipelineMetrics(new SimpleMeterRegistry())
        );
    }

    @AfterEach
    void tearDown() {
        crawlSourceExecutor.destroy();
    }

    @Test
    void scheduledCollectionContinuesWhenOneSourceFails() {
        BlogSource failing = source(1L, "failing");
        BlogSource succeeding = source(2L, "succeeding");
        when(transactionService.startRun()).thenReturn(10L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(failing, succeeding));
        when(sourceProcessor.prepare(1L, CrawlPolicy.recent()))
                .thenThrow(new IllegalStateException("network failed"));
        when(sourceProcessor.prepare(2L, CrawlPolicy.recent()))
                .thenReturn(prepared(2L));

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
        when(sourceProcessor.prepare(3L, CrawlPolicy.backfill(30)))
                .thenReturn(prepared(3L));

        ArticleCrawlResult result = crawlService.runSourceBackfill("uber", 30);

        assertThat(result.runId()).isEqualTo(11L);
        verify(sourceRepository, never()).findByEnabledTrue();
        verify(sourceProcessor).prepare(3L, CrawlPolicy.backfill(30));
    }

    @Test
    void allBackfillUsesEveryEnabledSourceAndRequestedLimit() {
        BlogSource first = source(4L, "first");
        BlogSource second = source(5L, "second");
        when(transactionService.startRun()).thenReturn(12L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(first, second));
        when(sourceProcessor.prepare(4L, CrawlPolicy.backfill(40)))
                .thenReturn(prepared(4L));
        when(sourceProcessor.prepare(5L, CrawlPolicy.backfill(40)))
                .thenReturn(prepared(5L));

        ArticleCrawlResult result = crawlService.runAllBackfill(40);

        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
        verify(sourceProcessor).prepare(4L, CrawlPolicy.backfill(40));
        verify(sourceProcessor).prepare(5L, CrawlPolicy.backfill(40));
    }

    @Test
    void allBackfillRunsSourcesUpToConfiguredConcurrency() throws Exception {
        BlogSource first = source(6L, "first");
        BlogSource second = source(7L, "second");
        BlogSource third = source(8L, "third");
        CountDownLatch firstTwoStarted = new CountDownLatch(2);
        CountDownLatch releaseTasks = new CountDownLatch(1);
        AtomicInteger activeCount = new AtomicInteger();
        AtomicInteger maxActiveCount = new AtomicInteger();
        AtomicInteger platformThreadCount = new AtomicInteger();
        when(transactionService.startRun()).thenReturn(13L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(first, second, third));
        when(sourceProcessor.prepare(anyLong(), any(CrawlPolicy.class)))
                .thenAnswer(invocation -> {
                    if (!Thread.currentThread().isVirtual()) {
                        platformThreadCount.incrementAndGet();
                    }
                    int active = activeCount.incrementAndGet();
                    maxActiveCount.accumulateAndGet(active, Math::max);
                    firstTwoStarted.countDown();
                    releaseTasks.await(2, TimeUnit.SECONDS);
                    activeCount.decrementAndGet();
                    Long sourceId = invocation.getArgument(0);
                    return prepared(sourceId);
                });

        CompletableFuture<ArticleCrawlResult> runResult = CompletableFuture.supplyAsync(
                () -> crawlService.runAllBackfill(50)
        );

        assertThat(firstTwoStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(activeCount).hasValue(2);
        assertThat(maxActiveCount).hasValue(2);
        assertThat(platformThreadCount).hasValue(2);
        releaseTasks.countDown();
        ArticleCrawlResult result = runResult.get(5, TimeUnit.SECONDS);

        assertThat(result.successCount()).isEqualTo(3);
        assertThat(result.sources())
                .extracting(SourceCrawlResult::sourceKey)
                .containsExactly("first", "second", "third");
    }

    @Test
    void allBackfillSerializesSourcesFromSameCompany() throws Exception {
        BlogSource first = source(9L, "first", 100L);
        BlogSource second = source(10L, "second", 100L);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        when(transactionService.startRun()).thenReturn(14L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(first, second));
        when(sourceProcessor.prepare(anyLong(), any(CrawlPolicy.class)))
                .thenAnswer(invocation -> {
                    Long sourceId = invocation.getArgument(0);
                    if (sourceId.equals(9L)) {
                        firstStarted.countDown();
                        releaseFirst.await(2, TimeUnit.SECONDS);
                        return prepared(9L);
                    }
                    secondStarted.countDown();
                    return prepared(10L);
                });

        CompletableFuture<ArticleCrawlResult> runResult = CompletableFuture.supplyAsync(
                () -> crawlService.runAllBackfill(50)
        );

        assertThat(firstStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(secondStarted.await(200, TimeUnit.MILLISECONDS)).isFalse();
        releaseFirst.countDown();
        ArticleCrawlResult result = runResult.get(5, TimeUnit.SECONDS);

        assertThat(secondStarted.getCount()).isZero();
        assertThat(result.successCount()).isEqualTo(2);
    }

    @Test
    void allBackfillPersistsOnlyAfterEverySourceIsPreparedAndUsesSingleWriter() {
        BlogSource first = source(11L, "first");
        BlogSource second = source(12L, "second");
        BlogSource third = source(13L, "third");
        AtomicInteger preparedCount = new AtomicInteger();
        AtomicInteger persistenceActive = new AtomicInteger();
        AtomicInteger maxPersistenceActive = new AtomicInteger();
        List<Long> persistedSourceIds = Collections.synchronizedList(new ArrayList<>());
        when(transactionService.startRun()).thenReturn(15L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(first, second, third));
        when(sourceProcessor.prepare(anyLong(), any(CrawlPolicy.class)))
                .thenAnswer(invocation -> {
                    preparedCount.incrementAndGet();
                    return prepared(invocation.getArgument(0));
                });
        doAnswer(invocation -> {
            assertThat(preparedCount).hasValue(3);
            int active = persistenceActive.incrementAndGet();
            maxPersistenceActive.accumulateAndGet(active, Math::max);
            Long sourceId = invocation.getArgument(1);
            persistedSourceIds.add(sourceId);
            persistenceActive.decrementAndGet();
            return SourceCrawlResult.success(
                    sourcesById.get(sourceId),
                    List.of(),
                    CrawlRunSummary.empty()
            );
        }).when(persistenceService).persistDiscoveredCandidates(
                anyLong(), anyLong(), any(), any()
        );

        ArticleCrawlResult result = crawlService.runAllBackfill(50);

        assertThat(result.successCount()).isEqualTo(3);
        assertThat(maxPersistenceActive).hasValue(1);
        assertThat(persistedSourceIds).containsExactly(11L, 12L, 13L);
    }

    @Test
    void persistenceFailureDoesNotStopLaterSources() {
        BlogSource failing = source(14L, "failing");
        BlogSource succeeding = source(15L, "succeeding");
        when(transactionService.startRun()).thenReturn(16L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(failing, succeeding));
        when(sourceProcessor.prepare(14L, CrawlPolicy.backfill(50))).thenReturn(prepared(14L));
        when(sourceProcessor.prepare(15L, CrawlPolicy.backfill(50))).thenReturn(prepared(15L));
        doThrow(new IllegalStateException("database failed"))
                .when(persistenceService)
                .persistDiscoveredCandidates(16L, 14L, List.of(), Map.of());

        ArticleCrawlResult result = crawlService.runAllBackfill(50);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        verify(transactionService).markSourceFailed(14L, "database failed");
        verify(persistenceService).persistDiscoveredCandidates(
                16L, 15L, List.of(), Map.of()
        );
    }

    @Test
    void concurrentRunsShareSinglePersistenceWriter() throws Exception {
        BlogSource source = source(16L, "shared");
        CountDownLatch firstPersistenceStarted = new CountDownLatch(1);
        CountDownLatch releasePersistence = new CountDownLatch(1);
        AtomicInteger persistenceActive = new AtomicInteger();
        AtomicInteger maxPersistenceActive = new AtomicInteger();
        when(transactionService.startRun()).thenReturn(17L, 18L);
        when(sourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(sourceProcessor.prepare(16L, CrawlPolicy.backfill(50)))
                .thenReturn(prepared(16L));
        doAnswer(invocation -> {
            int active = persistenceActive.incrementAndGet();
            maxPersistenceActive.accumulateAndGet(active, Math::max);
            firstPersistenceStarted.countDown();
            releasePersistence.await(2, TimeUnit.SECONDS);
            persistenceActive.decrementAndGet();
            return SourceCrawlResult.success(source, List.of(), CrawlRunSummary.empty());
        }).when(persistenceService).persistDiscoveredCandidates(
                anyLong(), anyLong(), any(), any()
        );

        CompletableFuture<ArticleCrawlResult> firstRun = CompletableFuture.supplyAsync(
                () -> crawlService.runAllBackfill(50)
        );
        CompletableFuture<ArticleCrawlResult> secondRun = CompletableFuture.supplyAsync(
                () -> crawlService.runAllBackfill(50)
        );

        assertThat(firstPersistenceStarted.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(200);
        assertThat(persistenceActive).hasValue(1);
        assertThat(maxPersistenceActive).hasValue(1);
        releasePersistence.countDown();

        assertThat(firstRun.get(5, TimeUnit.SECONDS).successCount()).isEqualTo(1);
        assertThat(secondRun.get(5, TimeUnit.SECONDS).successCount()).isEqualTo(1);
        assertThat(maxPersistenceActive).hasValue(1);
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
        return source(id, key, id);
    }

    private BlogSource source(Long id, String key, Long companyId) {
        Company company = Company.create(key, key);
        ReflectionTestUtils.setField(company, "id", companyId);
        BlogSource source = BlogSource.create(
                company,
                key,
                key,
                "https://example.com",
                "https://example.com/feed",
                CollectionMethod.RSS
        );
        ReflectionTestUtils.setField(source, "id", id);
        sourcesById.put(id, source);
        return source;
    }

    private PreparedSourceCrawl prepared(Long sourceId) {
        return new PreparedSourceCrawl(sourceId, List.of(), Map.of());
    }
}
