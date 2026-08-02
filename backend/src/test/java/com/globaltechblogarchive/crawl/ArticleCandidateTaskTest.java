package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateTask;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleCandidateTaskTest {

    @Test
    void candidateMovesThroughRetryAndApprovalStates() {
        ArticleCandidateTask task = task();
        LocalDateTime firstClaim = LocalDateTime.of(2026, 8, 1, 10, 0);

        task.claim(firstClaim, "v1");
        task.retryAt(firstClaim.plusMinutes(5), "OPENAI_HTTP_429", "rate limited");
        task.claim(firstClaim.plusMinutes(6), "v1");
        task.approve();

        assertThat(task.getStatus()).isEqualTo(ArticleCandidateDecisionStatus.AI_APPROVED);
        assertThat(task.getAttemptCount()).isEqualTo(2);
        assertThat(task.getProcessingStartedAt()).isNull();
        assertThat(task.getLastErrorCode()).isNull();
    }

    @Test
    void staleProcessingBecomesRetryWaiting() {
        ArticleCandidateTask task = task();
        LocalDateTime claimedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        task.claim(claimedAt, "v1");

        task.recover(claimedAt.plusMinutes(20));

        assertThat(task.getStatus()).isEqualTo(ArticleCandidateDecisionStatus.AI_RETRY_WAITING);
        assertThat(task.getLastErrorCode()).isEqualTo("STALE_PROCESSING");
    }

    @Test
    void newPromptVersionRequeuesPreviouslyProcessedCandidate() {
        ArticleCandidateTask task = task();
        LocalDateTime claimedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        task.claim(claimedAt, "v1");
        task.fail("OPENAI_RESPONSE_INVALID", "invalid response");

        task.observe(
                task.getSource(),
                candidate(),
                ArticleCandidateDecisionStatus.NEW,
                "v2"
        );

        assertThat(task.getStatus()).isEqualTo(ArticleCandidateDecisionStatus.NEW);
        assertThat(task.getAttemptCount()).isZero();
        assertThat(task.getProcessingPromptVersion()).isNull();
    }

    private ArticleCandidateTask task() {
        Company company = Company.create("test", "Test");
        BlogSource source = BlogSource.create(
                company,
                "test-source",
                "Test Source",
                "https://example.com",
                "https://example.com/feed",
                CollectionMethod.RSS
        );
        return ArticleCandidateTask.create(
                company,
                source,
                candidate(),
                ArticleCandidateDecisionStatus.NEW,
                "v1"
        );
    }

    private ArticleCandidate candidate() {
        return new ArticleCandidate(
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
        );
    }
}
