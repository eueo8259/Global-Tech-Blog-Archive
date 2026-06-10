package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.globaltechblogarchive.article.application.ArticleService;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.application.ApprovedArticleWriter;
import com.globaltechblogarchive.crawl.application.ArticleCandidateCollectorRegistry;
import com.globaltechblogarchive.crawl.application.ArticleCandidateFactory;
import com.globaltechblogarchive.crawl.application.ArticleCrawlService;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor;
import com.globaltechblogarchive.crawl.application.SourceCrawlProcessor;
import com.globaltechblogarchive.crawl.application.dto.ArticleCrawlResult;
import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.crawl.support.UrlHash;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import java.util.List;
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

    private ArticleCrawlService articleCrawlService;

    @BeforeEach
    void setUp() {
        lenient().when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(decisionRepository.save(any(ArticleAiDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(aiClient.model()).thenReturn("test-model");
        ArticleService articleService = new ArticleService(articleRepository);
        ApprovedArticleWriter approvedArticleWriter = new ApprovedArticleWriter(articleService);

        articleCrawlService = new ArticleCrawlService(
                blogSourceRepository,
                collectionRunRepository,
                new SourceCrawlProcessor(
                        decisionRepository,
                        collectionItemRepository,
                        collectorRegistry,
                        new ArticleCandidateFactory(articleRepository),
                        new ArticleDecisionProcessor(decisionRepository, aiClient, approvedArticleWriter)
                )
        );
    }

    @Test
    void runContinuesWhenOneSourceFails() {
        BlogSource failing = source(1L, "failing");
        BlogSource succeeding = source(2L, "succeeding");
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(1L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(failing, succeeding));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(failing)).thenThrow(new IllegalStateException("network failed"));
        when(collector.collect(succeeding)).thenReturn(List.of(new ParsedArticle(
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

        ArticleCrawlResult result = articleCrawlService.run();

        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.candidateCount()).isEqualTo(1);
        assertThat(result.storedCount()).isEqualTo(1);
        assertThat(result.aiApprovedCount()).isEqualTo(1);
        assertThat(result.runId()).isEqualTo(1L);
        assertThat(result.sources().getFirst().success()).isFalse();
        assertThat(result.sources().get(1).candidates()).hasSize(1);
        assertThat(failing.getLastErrorMsg()).isEqualTo("network failed");
        assertThat(succeeding.getLastCollectedAt()).isNotNull();
        verify(collectionItemRepository).saveAll(anyIterable());
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
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(2L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source)).thenReturn(List.of(approvedCard, rejectedCard));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of());
        when(aiClient.decide(anyList())).thenReturn(List.of(
                new ArticleMetadataDecision(0, "Inference scaling", ArticleCategory.AI, true, null),
                new ArticleMetadataDecision(1, "GPT-Rosalind introduction", ArticleCategory.ELSE, false, "PRODUCT_NEWS")
        ));

        ArticleCrawlResult result = articleCrawlService.run();

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
        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(articleCaptor.capture());
        Article article = articleCaptor.getValue();
        assertThat(article.getTitle()).isEqualTo("Inference scaling");
        assertThat(article.getCategory()).isEqualTo(ArticleCategory.AI);
        ArgumentCaptor<ArticleAiDecision> decisionCaptor = ArgumentCaptor.forClass(ArticleAiDecision.class);
        verify(decisionRepository, times(2)).save(decisionCaptor.capture());
        assertThat(decisionCaptor.getAllValues()).extracting(ArticleAiDecision::getModel)
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
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(3L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source)).thenReturn(List.of(approvedCard, rejectedCard));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of(
                        decision(source, approvedHash, true, ArticleCategory.AI),
                        decision(source, rejectedHash, false, ArticleCategory.ELSE)
                ));

        ArticleCrawlResult result = articleCrawlService.run();

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
        verify(articleRepository).save(any(Article.class));
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
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(4L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source)).thenReturn(List.of(card));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of());
        when(aiClient.decide(anyList())).thenThrow(new IllegalStateException("ai failed"));

        ArticleCrawlResult result = articleCrawlService.run();

        assertThat(result.sources().getFirst().candidates()).extracting(ArticleCandidate::decisionStatus)
                .containsExactly(ArticleCandidateDecisionStatus.AI_FAILED);
        assertThat(result.storedCount()).isZero();
        assertThat(result.aiFailedCount()).isEqualTo(1);
        verify(articleRepository, never()).save(any(Article.class));
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
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(5L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source)).thenReturn(List.of(card));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of());
        when(articleRepository.existsByCompanyIdAndArticleUrlHash(1L, hash)).thenReturn(true);

        ArticleCrawlResult result = articleCrawlService.run();

        ArticleCandidate candidate = result.sources().getFirst().candidates().getFirst();
        assertThat(candidate.duplicate()).isTrue();
        assertThat(candidate.decisionStatus()).isEqualTo(ArticleCandidateDecisionStatus.NEW);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.storedCount()).isZero();
        assertThat(result.aiApprovedCount()).isZero();
        verify(aiClient, never()).decide(anyList());
        verify(articleRepository, never()).save(any(Article.class));
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
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(6L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source)).thenReturn(List.of(card));
        when(decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(any(), anyList(), any()))
                .thenReturn(List.of(decision(source, hash, true, ArticleCategory.AI)));
        when(articleRepository.existsByCompanyIdAndArticleUrlHash(1L, hash)).thenReturn(true);

        ArticleCrawlResult result = articleCrawlService.run();

        ArticleCandidate candidate = result.sources().getFirst().candidates().getFirst();
        assertThat(candidate.duplicate()).isTrue();
        assertThat(candidate.decisionStatus()).isEqualTo(ArticleCandidateDecisionStatus.PREVIOUSLY_APPROVED);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.previouslyApprovedCount()).isZero();
        assertThat(result.storedCount()).isZero();
        verify(aiClient, never()).decide(anyList());
        verify(articleRepository, never()).save(any(Article.class));
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
}
