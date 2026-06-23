package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.application.ArticleService;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleDecisionProcessorTest {

    @Mock
    private ArticleAiDecisionRepository decisionRepository;

    @Mock
    private ArticleMetadataAiClient aiClient;

    @Mock
    private ArticleService articleService;

    private ArticleDecisionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ArticleDecisionProcessor(decisionRepository, aiClient, articleService);
        when(aiClient.model()).thenReturn("test-model");
        lenient().when(decisionRepository.save(any(ArticleAiDecision.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void processSplitsTwentyCandidatesIntoTenItemBatchesAndKeepsSuccessfulBatch() {
        List<ArticleCandidate> candidates = candidates(20);
        List<ArticleMetadataDecision> approved = decisions(10);
        when(aiClient.decide(anyList()))
                .thenThrow(new IllegalStateException("first batch failed"))
                .thenReturn(approved);

        ProcessedCandidates processed = processor.process(
                source(),
                candidates,
                new HashMap<>(),
                "v1"
        );

        ArgumentCaptor<List<ArticleMetadataInput>> inputs = ArgumentCaptor.forClass(List.class);
        verify(aiClient, times(2)).decide(inputs.capture());
        assertThat(inputs.getAllValues()).allSatisfy(batch -> assertThat(batch).hasSize(10));
        assertThat(inputs.getAllValues().getFirst().getFirst().shortContext()).isEqualTo("Context 0");
        assertThat(processed.candidates().subList(0, 10))
                .extracting(ArticleCandidate::decisionStatus)
                .containsOnly(ArticleCandidateDecisionStatus.AI_FAILED);
        assertThat(processed.candidates().subList(10, 20))
                .extracting(ArticleCandidate::decisionStatus)
                .containsOnly(ArticleCandidateDecisionStatus.AI_APPROVED);
        assertThat(processed.storedArticleCount()).isEqualTo(10);
        verify(decisionRepository, times(10)).save(any(ArticleAiDecision.class));
        verify(articleService, times(10)).save(any(Article.class));
    }

    @Test
    void processMarksOnlyMissingAiResponseIndexAsFailed() {
        List<ArticleCandidate> candidates = candidates(2);
        when(aiClient.decide(anyList())).thenReturn(List.of(
                new ArticleMetadataDecision(0, "Translated 0", ArticleCategory.AI, true, null)
        ));

        ProcessedCandidates processed = processor.process(
                source(),
                candidates,
                new HashMap<>(),
                "v1"
        );

        assertThat(processed.candidates()).extracting(ArticleCandidate::decisionStatus)
                .containsExactly(
                        ArticleCandidateDecisionStatus.AI_APPROVED,
                        ArticleCandidateDecisionStatus.AI_FAILED
                );
        assertThat(processed.storedArticleCount()).isEqualTo(1);
    }

    @Test
    void processPropagatesDecisionPersistenceFailureForSourceRollback() {
        when(aiClient.decide(anyList())).thenReturn(decisions(1));
        when(decisionRepository.save(any(ArticleAiDecision.class)))
                .thenThrow(new IllegalStateException("database failed"));

        assertThatThrownBy(() -> processor.process(
                source(),
                candidates(1),
                new HashMap<>(),
                "v1"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failed");
    }

    private List<ArticleCandidate> candidates(int count) {
        List<ArticleCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            candidates.add(new ArticleCandidate(
                    "openai",
                    "OpenAI",
                    "Title " + index,
                    "https://example.com/article-" + index,
                    LocalDateTime.of(2026, 6, 1, 10, 0).minusHours(index),
                    "Context " + index,
                    "hash-" + index,
                    false,
                    ArticleCandidateDecisionStatus.NEW,
                    List.of()
            ));
        }
        return candidates;
    }

    private List<ArticleMetadataDecision> decisions(int count) {
        List<ArticleMetadataDecision> decisions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            decisions.add(new ArticleMetadataDecision(
                    index,
                    "Translated " + index,
                    ArticleCategory.AI,
                    true,
                    null
            ));
        }
        return decisions;
    }

    private BlogSource source() {
        return BlogSource.create(
                Company.create("openai", "OpenAI"),
                "openai",
                "OpenAI News",
                "https://openai.com/news/",
                "https://openai.com/news/rss.xml",
                CollectionMethod.RSS
        );
    }
}
