package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.application.ArticleCandidateStateService;
import com.globaltechblogarchive.crawl.application.dto.ClaimedArticleCandidate;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateTask;
import com.globaltechblogarchive.crawl.repository.ArticleCandidateTaskRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleCandidateStateServiceTest {

    @Mock
    private ArticleCandidateTaskRepository candidateRepository;

    private ArticleCandidateStateService stateService;

    @BeforeEach
    void setUp() {
        stateService = new ArticleCandidateStateService(
                candidateRepository,
                new AiReviewProperties(
                        100,
                        10,
                        3,
                        Duration.ofMinutes(5),
                        Duration.ofMinutes(15)
                )
        );
    }

    @Test
    void claimNormalizesTimestampToDatabaseMicroseconds() {
        ArticleCandidateTask candidate = candidate();
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0, 0, 123_456_789);
        LocalDateTime databaseTime = LocalDateTime.of(2026, 8, 1, 10, 0, 0, 123_456_000);
        when(candidateRepository.findClaimable(
                eq(ArticleCandidateDecisionStatus.NEW),
                eq(ArticleCandidateDecisionStatus.AI_RETRY_WAITING),
                eq(databaseTime),
                any(Pageable.class)
        )).thenReturn(List.of(candidate));

        ClaimedArticleCandidate claimed = stateService.claimAvailable(now, "v1", 10).getFirst();

        assertThat(claimed.claimedAt()).isEqualTo(databaseTime);
        assertThat(candidate.getProcessingStartedAt()).isEqualTo(databaseTime);
    }

    @Test
    void staleCandidateFailsWhenMaximumAttemptsHaveBeenClaimed() {
        ArticleCandidateTask candidate = candidate();
        LocalDateTime first = LocalDateTime.of(2026, 8, 1, 10, 0);
        candidate.claim(first, "v1");
        candidate.recover(first.plusMinutes(16));
        candidate.claim(first.plusMinutes(16), "v1");
        candidate.recover(first.plusMinutes(32));
        candidate.claim(first.plusMinutes(32), "v1");
        when(candidateRepository.findStaleProcessing(
                ArticleCandidateDecisionStatus.AI_PROCESSING,
                first.plusMinutes(33)
        )).thenReturn(List.of(candidate));

        int recoveredCount = stateService.recoverStale(first.plusMinutes(48));

        assertThat(recoveredCount).isZero();
        assertThat(candidate.getStatus()).isEqualTo(ArticleCandidateDecisionStatus.AI_FAILED);
        assertThat(candidate.getLastErrorCode()).isEqualTo("STALE_PROCESSING_MAX_ATTEMPTS");
    }

    private ArticleCandidateTask candidate() {
        Company company = Company.create("test", "Test");
        ReflectionTestUtils.setField(company, "id", 1L);
        BlogSource source = BlogSource.create(
                company,
                "test-source",
                "Test Source",
                "https://example.com",
                "https://example.com/feed",
                CollectionMethod.RSS
        );
        ReflectionTestUtils.setField(source, "id", 2L);
        ArticleCandidateTask task = ArticleCandidateTask.create(
                company,
                source,
                new ArticleCandidate(
                        "test",
                        "Test",
                        "Original",
                        "https://example.com/article",
                        LocalDateTime.of(2026, 8, 1, 9, 0),
                        "Context",
                        "hash",
                        false,
                        ArticleCandidateDecisionStatus.NEW,
                        List.of()
                ),
                ArticleCandidateDecisionStatus.NEW,
                "v1"
        );
        ReflectionTestUtils.setField(task, "id", 3L);
        return task;
    }
}
