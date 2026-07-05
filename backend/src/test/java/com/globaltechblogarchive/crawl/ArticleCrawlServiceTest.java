package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.application.ArticleCandidateCollectorRegistry;
import com.globaltechblogarchive.crawl.application.ArticleCandidateFactory;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor;
import com.globaltechblogarchive.crawl.application.SourceCrawlProcessor;
import com.globaltechblogarchive.crawl.application.CrawlTransactionService;
import com.globaltechblogarchive.crawl.application.CrawlPersistenceService;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.crawl.support.UrlHash;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleCrawlServiceTest {

    @Mock
    private BlogSourceRepository blogSourceRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleAiDecisionRepository decisionRepository;

    @Mock
    private ArticleMetadataAiClient aiClient;

    @Mock
    private ArticleCollectionRunRepository collectionRunRepository;

    @Mock
    private ArticleDiscoveryLogRepository collectionItemRepository;

    @Mock
    private ArticleCandidateCollectorRegistry collectorRegistry;

    @Mock
    private ArticleCandidateCollector collector;

    @Mock
    private CrawlTransactionService transactionService;

    private ArticleCrawlService articleCrawlService;
    private final Map<Long, BlogSource> sourcesById = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(decisionRepository.save(any(ArticleAiDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(aiClient.model()).thenReturn("test-model");
        lenient().when(transactionService.startRun()).thenReturn(1L);
        lenient().when(collectionRunRepository.findById(any())).thenAnswer(invocation ->
                Optional.of(run(invocation.getArgument(0))));
        lenient().when(blogSourceRepository.findWithCompanyById(any())).thenAnswer(invocation ->
                Optional.ofNullable(sourcesById.get(invocation.getArgument(0))));
        CrawlPersistenceService persistenceService = new CrawlPersistenceService(
                articleRepository,
                decisionRepository,
                collectionItemRepository,
                collectionRunRepository,
                blogSourceRepository
        );

        articleCrawlService = new ArticleCrawlService(
                blogSourceRepository,
                collectionItemRepository,
                new SourceCrawlProcessor(
                        decisionRepository,
                        collectionItemRepository,
                        collectorRegistry,
                        new ArticleCandidateFactory(articleRepository),
                        new ArticleDecisionProcessor(aiClient),
                        persistenceService,
                        blogSourceRepository
                ),
                transactionService
        );
    }

    @Test
    void retryAiFailuresProcessesOnlyUnresolvedCandidatesReturnedByRepository() {
        BlogSource source = source(1L, "openai");
        ArticleDiscoveryLog failure = failedLog(10L, source, "hash-failed");
        when(collectionItemRepository.findUnresolvedAiFailures(
                any(),
                any(),
                any()
        )).thenReturn(List.of(failure));
        when(collectionItemRepository.findAllById(anyIterable())).thenReturn(List.of(failure));
        when(aiClient.decide(anyList())).thenReturn(List.of(
                new ArticleMetadataDecision(0, "Translated", ArticleCategory.ELSE, false, "NOT_ENGINEERING")
        ));

        ArticleCrawlResult result = articleCrawlService.retryAiFailures(20);

        assertThat(result.runId()).isEqualTo(1L);
        assertThat(result.sourceCount()).isEqualTo(1);
        assertThat(result.candidateCount()).isEqualTo(1);
        assertThat(result.aiRejectedCount()).isEqualTo(1);
        assertThat(result.aiFailedCount()).isZero();
        assertThat(result.storedCount()).isZero();
        verify(decisionRepository).saveAll(anyIterable());
        verify(collectionItemRepository).saveAll(anyIterable());
        verify(transactionService).completeRun(1L, 1, 1, 0, result.sources().getFirst().summary());
    }

    @Test
    void retryAiFailuresRejectsLimitOutsideAllowedRange() {
        assertThatThrownBy(() -> articleCrawlService.retryAiFailures(0))
                .isInstanceOfSatisfying(InvalidInputException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE))
                .hasMessage("AI failure retry limit must be between 1 and 100");

        verify(collectionItemRepository, never()).findUnresolvedAiFailures(any(), any(), any());
        verify(transactionService, never()).startRun();
    }

    @Test
    void runContinuesWhenOneSourceFails() {
        BlogSource failing = source(1L, "failing");
        BlogSource succeeding = source(2L, "succeeding");
        when(transactionService.startRun()).thenReturn(1L);
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(failing, succeeding));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(failing, CrawlMode.RECENT)).thenThrow(new IllegalStateException("network failed"));
        when(collector.collect(succeeding, CrawlMode.RECENT)).thenReturn(List.of(new ParsedArticle(
                "Scaling systems",
                "https://example.com/scaling?utm_source=test#section",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                "Architecture context"
        )));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of());
        when(aiClient.decide(anyList())).thenReturn(List.of(
                new ArticleMetadataDecision(0, "Translated scaling systems", ArticleCategory.ARCHITECTURE, true, null)
        ));

        ArticleCrawlResult result = articleCrawlService.runScheduled();

        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.candidateCount()).isEqualTo(1);
        assertThat(result.storedCount()).isEqualTo(1);
        assertThat(result.aiApprovedCount()).isEqualTo(1);
        assertThat(result.runId()).isEqualTo(1L);
        assertThat(result.sources().getFirst().success()).isFalse();
        assertThat(result.sources().get(1).candidates()).hasSize(1);
        verify(transactionService).markSourceFailed(1L, "network failed");
        assertThat(succeeding.getLastCollectedAt()).isNotNull();
        verify(collectionItemRepository).saveAll(anyIterable());
    }

    @Test
    void runScheduledUsesRecentCollectionMode() {
        BlogSource source = source(1L, "openai");
        when(transactionService.startRun()).thenReturn(7L);
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.RECENT)).thenReturn(List.of());

        ArticleCrawlResult result = articleCrawlService.runScheduled();

        assertThat(result.runId()).isEqualTo(7L);
        assertThat(result.sourceCount()).isEqualTo(1);
        verify(collector).collect(source, CrawlMode.RECENT);
        verify(transactionService).completeRun(7L, 1, 1, 0, CrawlRunSummary.empty());
    }

    @Test
    void runSourceBackfillUsesOnlyRequestedSource() {
        BlogSource source = source(1L, "uber");
        when(transactionService.startRun()).thenReturn(8L);
        when(blogSourceRepository.findBySourceKeyAndEnabledTrue("uber")).thenReturn(Optional.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.BACKFILL)).thenReturn(List.of());

        ArticleCrawlResult result = articleCrawlService.runSourceBackfill("uber");

        assertThat(result.runId()).isEqualTo(8L);
        assertThat(result.sourceCount()).isEqualTo(1);
        assertThat(result.sources().getFirst().sourceKey()).isEqualTo("uber");
        verify(blogSourceRepository, never()).findByEnabledTrue();
        verify(collector).collect(source, CrawlMode.BACKFILL);
        verify(transactionService).completeRun(8L, 1, 1, 0, CrawlRunSummary.empty());
    }

    @Test
    void runSourceBackfillUsesBackfillModeForRequestedSource() {
        BlogSource source = source(1L, "uber");
        when(transactionService.startRun()).thenReturn(9L);
        when(blogSourceRepository.findBySourceKeyAndEnabledTrue("uber")).thenReturn(Optional.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.BACKFILL)).thenReturn(List.of());

        ArticleCrawlResult result = articleCrawlService.runSourceBackfill("uber");

        assertThat(result.runId()).isEqualTo(9L);
        assertThat(result.sourceCount()).isEqualTo(1);
        verify(collector).collect(source, CrawlMode.BACKFILL);
    }

    @Test
    void runSourceThrowsInvalidInputWhenEnabledSourceDoesNotExist() {
        when(blogSourceRepository.findBySourceKeyAndEnabledTrue("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleCrawlService.runSourceBackfill("missing"))
                .isInstanceOfSatisfying(InvalidInputException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE))
                .hasMessage("Enabled source not found: missing");

        verify(transactionService, never()).startRun();
    }

    @Test
    void runStoresOnlyAiApprovedCandidatesAsArticles() {
        BlogSource source = source(1L, "openai");
        ParsedArticle approvedCard = new ParsedArticle(
                "How we scaled inference",
                "https://openai.com/news/approved",
                null,
                ""
        );
        ParsedArticle rejectedCard = new ParsedArticle(
                "Introducing GPT-Rosalind",
                "https://openai.com/news/rejected",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                ""
        );
        when(transactionService.startRun()).thenReturn(2L);
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.RECENT)).thenReturn(List.of(approvedCard, rejectedCard));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of());
        when(aiClient.decide(anyList())).thenReturn(List.of(
                new ArticleMetadataDecision(0, "Inference scaling", ArticleCategory.AI, true, null),
                new ArticleMetadataDecision(1, "GPT-Rosalind introduction", ArticleCategory.ELSE, false, "PRODUCT_NEWS")
        ));

        ArticleCrawlResult result = articleCrawlService.runScheduled();

        List<ArticleCandidate> candidates = result.sources().getFirst().candidates();
        assertThat(candidates).extracting(ArticleCandidate::decisionStatus)
                .containsExactly(
                        ArticleCandidateDecisionStatus.AI_APPROVED,
                        ArticleCandidateDecisionStatus.AI_REJECTED
                );
        assertThat(result.duplicateCount()).isZero();
        assertThat(result.storedCount()).isEqualTo(1);
        assertThat(result.aiApprovedCount()).isEqualTo(1);
        assertThat(result.aiRejectedCount()).isEqualTo(1);
        assertThat(result.aiFailedCount()).isZero();
        assertThat(result.previouslyApprovedCount()).isZero();
        assertThat(result.previouslyRejectedCount()).isZero();
        ArgumentCaptor<Iterable<Article>> articleCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(articleRepository).saveAll(articleCaptor.capture());
        assertThat(articleCaptor.getValue()).singleElement().satisfies(article -> {
            assertThat(article.getTitle()).isEqualTo("Inference scaling");
            assertThat(article.getCategory()).isEqualTo(ArticleCategory.AI);
        });
        ArgumentCaptor<Iterable<ArticleAiDecision>> decisionCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(decisionRepository).saveAll(decisionCaptor.capture());
        assertThat(decisionCaptor.getValue()).extracting(ArticleAiDecision::getModel)
                .containsOnly("test-model");
        verify(collectionItemRepository).saveAll(anyIterable());
    }

    @Test
    void runSkipsAiForPreviousRejectedAndStoresPreviousApprovedArticle() {
        BlogSource source = source(1L, "openai");
        ParsedArticle approvedCard = new ParsedArticle(
                "Approved post",
                "https://openai.com/news/approved",
                null,
                ""
        );
        ParsedArticle rejectedCard = new ParsedArticle(
                "Rejected post",
                "https://openai.com/news/rejected",
                null,
                ""
        );
        String approvedHash = UrlHash.sha256(UrlNormalizer.normalize(approvedCard.originalUrl()));
        String rejectedHash = UrlHash.sha256(UrlNormalizer.normalize(rejectedCard.originalUrl()));
        when(transactionService.startRun()).thenReturn(3L);
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.RECENT)).thenReturn(List.of(approvedCard, rejectedCard));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of(
                        decision(source, approvedHash, true, ArticleCategory.AI),
                        decision(source, rejectedHash, false, ArticleCategory.ELSE)
                ));

        ArticleCrawlResult result = articleCrawlService.runScheduled();

        assertThat(result.sources().getFirst().candidates()).extracting(ArticleCandidate::decisionStatus)
                .containsExactly(
                        ArticleCandidateDecisionStatus.PREVIOUSLY_APPROVED,
                        ArticleCandidateDecisionStatus.PREVIOUSLY_REJECTED
                );
        assertThat(result.storedCount()).isEqualTo(1);
        assertThat(result.aiApprovedCount()).isZero();
        assertThat(result.aiRejectedCount()).isZero();
        assertThat(result.aiFailedCount()).isZero();
        assertThat(result.previouslyApprovedCount()).isEqualTo(1);
        assertThat(result.previouslyRejectedCount()).isEqualTo(1);
        verify(aiClient, never()).decide(anyList());
        verify(articleRepository).saveAll(anyIterable());
    }

    @Test
    void runMarksNewCandidatesFailedWhenAiClientFails() {
        BlogSource source = source(1L, "openai");
        ParsedArticle card = new ParsedArticle(
                "How we scaled inference",
                "https://openai.com/news/failed",
                null,
                ""
        );
        when(transactionService.startRun()).thenReturn(4L);
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.RECENT)).thenReturn(List.of(card));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of());
        when(aiClient.decide(anyList())).thenThrow(new IllegalStateException("ai failed"));

        ArticleCrawlResult result = articleCrawlService.runScheduled();

        assertThat(result.sources().getFirst().candidates()).extracting(ArticleCandidate::decisionStatus)
                .containsExactly(ArticleCandidateDecisionStatus.AI_FAILED);
        assertThat(result.storedCount()).isZero();
        assertThat(result.aiFailedCount()).isEqualTo(1);
        verify(articleRepository, never()).saveAll(anyIterable());
        verify(collectionItemRepository).saveAll(anyIterable());
    }

    @Test
    void runSkipsAiAndArticleSaveWhenArticleAlreadyExists() {
        BlogSource source = source(1L, "openai");
        ParsedArticle card = new ParsedArticle(
                "How we scaled inference",
                "https://openai.com/news/duplicate",
                null,
                ""
        );
        String hash = UrlHash.sha256(UrlNormalizer.normalize(card.originalUrl()));
        when(transactionService.startRun()).thenReturn(5L);
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.RECENT)).thenReturn(List.of(card));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of());
        when(articleRepository.existsByCompanyIdAndArticleUrlHash(1L, hash)).thenReturn(true);

        ArticleCrawlResult result = articleCrawlService.runScheduled();

        ArticleCandidate candidate = result.sources().getFirst().candidates().getFirst();
        assertThat(candidate.duplicate()).isTrue();
        assertThat(candidate.decisionStatus()).isEqualTo(ArticleCandidateDecisionStatus.NEW);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.storedCount()).isZero();
        assertThat(result.aiApprovedCount()).isZero();
        verify(aiClient, never()).decide(anyList());
        verify(articleRepository, never()).saveAll(anyIterable());
        verify(collectionItemRepository).saveAll(anyIterable());
    }

    @Test
    void runSkipsArticleSaveWhenPreviousApprovedDecisionAlreadyHasArticle() {
        BlogSource source = source(1L, "openai");
        ParsedArticle card = new ParsedArticle(
                "Approved post",
                "https://openai.com/news/approved-duplicate",
                null,
                ""
        );
        String hash = UrlHash.sha256(UrlNormalizer.normalize(card.originalUrl()));
        when(transactionService.startRun()).thenReturn(6L);
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source, CrawlMode.RECENT)).thenReturn(List.of(card));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of(decision(source, hash, true, ArticleCategory.AI)));
        when(articleRepository.existsByCompanyIdAndArticleUrlHash(1L, hash)).thenReturn(true);

        ArticleCrawlResult result = articleCrawlService.runScheduled();

        ArticleCandidate candidate = result.sources().getFirst().candidates().getFirst();
        assertThat(candidate.duplicate()).isTrue();
        assertThat(candidate.decisionStatus()).isEqualTo(ArticleCandidateDecisionStatus.PREVIOUSLY_APPROVED);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.previouslyApprovedCount()).isZero();
        assertThat(result.storedCount()).isZero();
        verify(aiClient, never()).decide(anyList());
        verify(articleRepository, never()).saveAll(anyIterable());
        verify(collectionItemRepository).saveAll(anyIterable());
    }

    private ArticleCollectionRun run(Long id) {
        ArticleCollectionRun run = ArticleCollectionRun.start(LocalDateTime.of(2026, 6, 1, 9, 0));
        ReflectionTestUtils.setField(run, "id", id);
        return run;
    }

    private BlogSource source(Long id, String companyKey) {
        Company company = Company.create(companyKey, companyKey);
        ReflectionTestUtils.setField(company, "id", id);
        BlogSource source = BlogSource.create(
                company,
                companyKey,
                companyKey,
                "https://example.com/",
                "https://example.com/feed",
                CollectionMethod.RSS
        );
        ReflectionTestUtils.setField(source, "id", id);
        sourcesById.put(id, source);
        return source;
    }

    private ArticleAiDecision decision(
            BlogSource source,
            String articleUrlHash,
            boolean saveTarget,
            ArticleCategory category
    ) {
        return ArticleAiDecision.create(
                source.getCompany(),
                articleUrlHash,
                "https://example.com/" + articleUrlHash,
                "Original",
                "Translated",
                category,
                saveTarget,
                "gpt-5-mini",
                "v1"
        );
    }

    private ArticleDiscoveryLog failedLog(Long id, BlogSource source, String articleUrlHash) {
        ArticleCandidate candidate = new ArticleCandidate(
                source.getCompany().getCompanyKey(),
                source.getCompany().getCompanyName(),
                "Failed article",
                "https://example.com/" + articleUrlHash,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                "Context",
                articleUrlHash,
                false,
                ArticleCandidateDecisionStatus.AI_FAILED,
                List.of()
        );
        ArticleDiscoveryLog log = ArticleDiscoveryLog.create(run(99L), source, candidate);
        ReflectionTestUtils.setField(log, "id", id);
        return log;
    }
}
