package com.globaltechblogarchive.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.collection.application.ArticleCrawlResult;
import com.globaltechblogarchive.collection.application.ArticleCrawlService;
import com.globaltechblogarchive.collection.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.collection.collector.ArticleCandidateCollectorRegistry;
import com.globaltechblogarchive.collection.domain.ArticleCandidate;
import com.globaltechblogarchive.collection.domain.ArticleCollectionRun;
import com.globaltechblogarchive.collection.parser.ParsedArticleCard;
import com.globaltechblogarchive.collection.repository.ArticleCollectionItemRepository;
import com.globaltechblogarchive.collection.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.collection.support.UrlHash;
import com.globaltechblogarchive.collection.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private ArticleCollectionRunRepository collectionRunRepository;

    @Mock
    private ArticleCollectionItemRepository collectionItemRepository;

    @Mock
    private ArticleCandidateCollectorRegistry collectorRegistry;

    @Mock
    private ArticleCandidateCollector collector;

    @InjectMocks
    private ArticleCrawlService articleCrawlService;

    @Test
    void runContinuesWhenOneSourceFails() {
        BlogSource failing = source(1L, "failing");
        BlogSource succeeding = source(2L, "succeeding");
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(1L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(failing, succeeding));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(failing)).thenThrow(new IllegalStateException("network failed"));
        when(collector.collect(succeeding)).thenReturn(List.of(new ParsedArticleCard(
                "Scaling systems",
                "https://example.com/scaling?utm_source=test#section",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                "Architecture context"
        )));

        ArticleCrawlResult result = articleCrawlService.run();

        assertThat(result.sourceCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.candidateCount()).isEqualTo(1);
        assertThat(result.storedCount()).isEqualTo(1);
        assertThat(result.runId()).isEqualTo(1L);
        assertThat(result.sources().getFirst().success()).isFalse();
        assertThat(result.sources().get(1).candidates()).hasSize(1);
        assertThat(failing.getLastErrorMsg()).isEqualTo("network failed");
        assertThat(succeeding.getLastCollectedAt()).isNotNull();
        verify(collectionItemRepository).saveAll(anyIterable());
    }

    @Test
    void runMarksDuplicateCandidates() {
        BlogSource source = source(1L, "openai");
        ParsedArticleCard card = new ParsedArticleCard(
                "Engineering post",
                "https://openai.com/news/post",
                null,
                ""
        );
        String hash = UrlHash.sha256(UrlNormalizer.normalize(card.originalUrl()));
        when(collectionRunRepository.save(any(ArticleCollectionRun.class))).thenAnswer(invocation -> run(2L));
        when(blogSourceRepository.findByEnabledTrue()).thenReturn(List.of(source));
        when(collectorRegistry.find(CollectionMethod.RSS)).thenReturn(collector);
        when(collector.collect(source)).thenReturn(List.of(card));
        when(articleRepository.existsBySourceIdAndNormalizedUrlHash(1L, hash)).thenReturn(true);

        ArticleCrawlResult result = articleCrawlService.run();

        ArticleCandidate candidate = result.sources().getFirst().candidates().getFirst();
        assertThat(candidate.duplicate()).isTrue();
        assertThat(candidate.shortContext()).isEqualTo("Engineering post");
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.storedCount()).isEqualTo(1);
        verify(articleRepository).existsBySourceIdAndNormalizedUrlHash(1L, hash);
    }

    private ArticleCollectionRun run(Long id) {
        ArticleCollectionRun run = ArticleCollectionRun.start(LocalDateTime.of(2026, 6, 1, 9, 0));
        ReflectionTestUtils.setField(run, "id", id);
        return run;
    }

    private BlogSource source(Long id, String companyKey) {
        BlogSource source = BlogSource.create(
                companyKey,
                companyKey,
                "https://example.com/",
                "https://example.com/feed",
                CollectionMethod.RSS
        );
        ReflectionTestUtils.setField(source, "id", id);
        return source;
    }
}
