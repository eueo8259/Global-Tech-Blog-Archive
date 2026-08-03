package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiRequestException;
import com.globaltechblogarchive.crawl.application.ArticleAiReviewResultService;
import com.globaltechblogarchive.crawl.application.ArticleAiReviewService;
import com.globaltechblogarchive.crawl.application.ArticleCandidateStateService;
import com.globaltechblogarchive.crawl.application.dto.AiReviewCandidateResult;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import com.globaltechblogarchive.crawl.application.dto.ClaimedArticleCandidate;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ArticleAiReviewServiceTest {

    @Mock
    private ArticleMetadataAiClient aiClient;

    @Mock
    private ArticleCandidateStateService stateService;

    @Mock
    private ArticleAiReviewResultService resultService;

    private ArticleAiReviewService reviewService;
    private ClaimedArticleCandidate claimed;

    @BeforeEach
    void setUp() {
        AiReviewProperties properties = new AiReviewProperties(
                50,
                10,
                3,
                100,
                Duration.ofMinutes(5),
                Duration.ofMinutes(15)
        );
        reviewService = new ArticleAiReviewService(aiClient, stateService, resultService, properties);
        LocalDateTime claimedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        claimed = new ClaimedArticleCandidate(
                1L,
                2L,
                3L,
                "https://example.com/article",
                "hash",
                "Original",
                "Context",
                null,
                claimedAt.minusDays(1),
                1,
                "v1",
                claimedAt
        );
    }

    @Test
    void openAiCallRunsWithoutActiveDatabaseTransaction() {
        when(stateService.claimAvailable(any(), eq("v1"), eq(50))).thenReturn(List.of(claimed));
        when(aiClient.decide(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(new ArticleMetadataDecision(
                    0,
                    "Translated",
                    ArticleCategory.AI,
                    true,
                    null
            ));
        });
        when(aiClient.model()).thenReturn("gpt-test");
        when(resultService.complete(any(), any(), eq("gpt-test")))
                .thenReturn(AiReviewCandidateResult.approved(true));

        AiReviewRunResult result = reviewService.runScheduled();

        assertThat(result.approvedCount()).isEqualTo(1);
        assertThat(result.storedCount()).isEqualTo(1);
    }

    @Test
    void transientOpenAiFailureMovesWholeBatchToRetryPath() {
        when(stateService.claimAvailable(any(), eq("v1"), eq(50))).thenReturn(List.of(claimed));
        when(aiClient.decide(any())).thenThrow(new ArticleMetadataAiRequestException(
                "OPENAI_HTTP_429",
                "rate limited",
                true
        ));
        when(resultService.fail(
                eq(claimed),
                any(),
                eq(true),
                eq("OPENAI_HTTP_429"),
                anyString()
        )).thenReturn(AiReviewCandidateResult.retryWaiting());

        AiReviewRunResult result = reviewService.runScheduled();

        assertThat(result.retryWaitingCount()).isEqualTo(1);
        verify(resultService).fail(
                eq(claimed),
                any(),
                eq(true),
                eq("OPENAI_HTTP_429"),
                eq("rate limited")
        );
    }

    @Test
    void missingDecisionIsPermanentFailure() {
        when(stateService.claimAvailable(any(), eq("v1"), eq(50))).thenReturn(List.of(claimed));
        when(aiClient.decide(any())).thenReturn(List.of());
        when(resultService.fail(
                eq(claimed),
                any(),
                eq(false),
                eq("OPENAI_RESPONSE_MISSING"),
                anyString()
        )).thenReturn(AiReviewCandidateResult.failed());

        AiReviewRunResult result = reviewService.runScheduled();

        assertThat(result.failedCount()).isEqualTo(1);
    }
}
