package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.application.ArticleAiReviewResultService;
import com.globaltechblogarchive.crawl.application.ArticleCandidateStateService;
import com.globaltechblogarchive.crawl.application.dto.AiReviewCandidateResult;
import com.globaltechblogarchive.crawl.application.dto.ClaimedArticleCandidate;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateTask;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCandidateTaskRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({
        ArticleCandidateStateService.class,
        ArticleAiReviewResultService.class,
        ArticleAiReviewPersistenceTest.TestConfig.class
})
class ArticleAiReviewPersistenceTest extends MySqlIntegrationTest {

    @Autowired
    private ArticleCandidateStateService stateService;

    @Autowired
    private ArticleAiReviewResultService resultService;

    @Autowired
    private ArticleCandidateTaskRepository candidateRepository;

    @Autowired
    private ArticleAiDecisionRepository decisionRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void persistedNewCandidateCanBeClaimedByLaterAiExecution() {
        CandidateIds ids = persistNewCandidate();
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);

        ClaimedArticleCandidate claimed = claim(ids.candidateId(), now);

        assertThat(claimed.attemptCount()).isEqualTo(1);
        assertThat(candidate(ids.candidateId()).getStatus())
                .isEqualTo(ArticleCandidateDecisionStatus.AI_PROCESSING);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void approvalCommitsDecisionArticleAndCandidateStatusTogether() {
        CandidateIds ids = persistNewCandidate();
        ClaimedArticleCandidate claimed = claim(ids.candidateId(), LocalDateTime.of(2026, 8, 2, 10, 0));

        AiReviewCandidateResult result = resultService.complete(
                claimed,
                approvedDecision("Translated title"),
                "gpt-test"
        );

        assertThat(result.storedCount()).isEqualTo(1);
        assertThat(candidate(ids.candidateId()).getStatus())
                .isEqualTo(ArticleCandidateDecisionStatus.AI_APPROVED);
        assertThat(decisionRepository.existsByCompanyIdAndArticleUrlHashAndPromptVersion(
                ids.companyId(),
                ids.hash(),
                "v1"
        )).isTrue();
        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(ids.companyId(), ids.hash()))
                .isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void claimWithNanosecondsCanCompleteAfterDatabaseRoundTrip() {
        CandidateIds ids = persistNewCandidate();
        LocalDateTime claimTime = LocalDateTime.of(2026, 8, 2, 10, 0, 0, 123_456_789);
        ClaimedArticleCandidate claimed = claim(ids.candidateId(), claimTime);

        AiReviewCandidateResult result = resultService.complete(
                claimed,
                approvedDecision("Translated title"),
                "gpt-test"
        );

        assertThat(claimed.claimedAt().getNano()).isEqualTo(123_456_000);
        assertThat(result.approvedCount()).isEqualTo(1);
        assertThat(candidate(ids.candidateId()).getStatus())
                .isEqualTo(ArticleCandidateDecisionStatus.AI_APPROVED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectionCommitsDecisionAndCandidateWithoutArticle() {
        CandidateIds ids = persistNewCandidate();
        ClaimedArticleCandidate claimed = claim(ids.candidateId(), LocalDateTime.of(2026, 8, 2, 11, 0));

        AiReviewCandidateResult result = resultService.complete(
                claimed,
                new ArticleMetadataDecision(0, "Translated title", ArticleCategory.ELSE, false, "PRODUCT_NEWS"),
                "gpt-test"
        );

        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(candidate(ids.candidateId()).getStatus())
                .isEqualTo(ArticleCandidateDecisionStatus.AI_REJECTED);
        assertThat(decisionRepository.existsByCompanyIdAndArticleUrlHashAndPromptVersion(
                ids.companyId(),
                ids.hash(),
                "v1"
        )).isTrue();
        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(ids.companyId(), ids.hash()))
                .isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void approvalPersistenceFailureRollsBackDecisionArticleAndStatus() {
        CandidateIds ids = persistNewCandidate();
        ClaimedArticleCandidate claimed = claim(ids.candidateId(), LocalDateTime.of(2026, 8, 3, 10, 0));

        assertThatThrownBy(() -> resultService.complete(
                claimed,
                approvedDecision(null),
                "gpt-test"
        )).isInstanceOf(RuntimeException.class);

        assertThat(candidate(ids.candidateId()).getStatus())
                .isEqualTo(ArticleCandidateDecisionStatus.AI_PROCESSING);
        assertThat(decisionRepository.existsByCompanyIdAndArticleUrlHashAndPromptVersion(
                ids.companyId(),
                ids.hash(),
                "v1"
        )).isFalse();
        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(ids.companyId(), ids.hash()))
                .isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transientFailureRetriesThenFailsAtMaximumAttempts() {
        CandidateIds ids = persistNewCandidate();
        LocalDateTime first = LocalDateTime.of(2026, 8, 4, 10, 0);
        ClaimedArticleCandidate firstClaim = claim(ids.candidateId(), first);

        AiReviewCandidateResult firstFailure = resultService.fail(
                firstClaim,
                first,
                true,
                "OPENAI_HTTP_500",
                "server error"
        );
        ClaimedArticleCandidate secondClaim = claim(ids.candidateId(), first.plusMinutes(6));
        resultService.fail(secondClaim, first.plusMinutes(6), true, "OPENAI_HTTP_500", "server error");
        ClaimedArticleCandidate thirdClaim = claim(ids.candidateId(), first.plusMinutes(12));
        AiReviewCandidateResult finalFailure = resultService.fail(
                thirdClaim,
                first.plusMinutes(12),
                true,
                "OPENAI_HTTP_500",
                "server error"
        );

        assertThat(firstFailure.retryWaitingCount()).isEqualTo(1);
        assertThat(finalFailure.failedCount()).isEqualTo(1);
        assertThat(candidate(ids.candidateId()).getStatus())
                .isEqualTo(ArticleCandidateDecisionStatus.AI_FAILED);
        assertThat(candidate(ids.candidateId()).getAttemptCount()).isEqualTo(3);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleProcessingIsRecoveredAndOldWorkerResultIsIgnored() {
        CandidateIds ids = persistNewCandidate();
        LocalDateTime first = LocalDateTime.of(2026, 8, 5, 10, 0);
        ClaimedArticleCandidate oldClaim = claim(ids.candidateId(), first);

        assertThat(stateService.recoverStale(first.plusMinutes(16))).isGreaterThanOrEqualTo(1);
        ClaimedArticleCandidate newClaim = claim(ids.candidateId(), first.plusMinutes(16));
        AiReviewCandidateResult oldResult = resultService.complete(
                oldClaim,
                approvedDecision("Late result"),
                "gpt-test"
        );

        assertThat(oldResult).isEqualTo(AiReviewCandidateResult.skipped());
        assertThat(candidate(ids.candidateId()).matchesClaim(newClaim.claimedAt(), "v1")).isTrue();
        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(ids.companyId(), ids.hash()))
                .isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void repeatedStaleRecoveryFailsCandidateAtMaximumAttempts() {
        CandidateIds ids = persistNewCandidate();
        LocalDateTime first = LocalDateTime.of(2026, 8, 6, 10, 0);
        claim(ids.candidateId(), first);

        stateService.recoverStale(first.plusMinutes(16));
        claim(ids.candidateId(), first.plusMinutes(16));
        stateService.recoverStale(first.plusMinutes(32));
        claim(ids.candidateId(), first.plusMinutes(32));
        stateService.recoverStale(first.plusMinutes(48));

        ArticleCandidateTask candidate = candidate(ids.candidateId());
        assertThat(candidate.getStatus()).isEqualTo(ArticleCandidateDecisionStatus.AI_FAILED);
        assertThat(candidate.getAttemptCount()).isEqualTo(3);
        assertThat(candidate.getLastErrorCode()).isEqualTo("STALE_PROCESSING_MAX_ATTEMPTS");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void missingPublishedAtUsesCandidateCollectionTimeAfterDelayedApproval() {
        CandidateIds ids = persistNewCandidate(null);
        LocalDateTime collectedAt = candidate(ids.candidateId()).getCreatedAt();
        ClaimedArticleCandidate claimed = claim(
                ids.candidateId(),
                collectedAt.plusDays(3)
        );

        resultService.complete(claimed, approvedDecision("Translated title"), "gpt-test");

        Article article = articleRepository.findAllByOrderByIdAsc()
                .stream()
                .filter(stored -> stored.getCompany().getId().equals(ids.companyId()))
                .filter(stored -> stored.getArticleUrlHash().equals(ids.hash()))
                .findFirst()
                .orElseThrow();
        assertThat(article.getPublishedAt()).isEqualTo(collectedAt);
    }

    private ClaimedArticleCandidate claim(Long candidateId, LocalDateTime now) {
        return stateService.claimAvailable(now, "v1", 100)
                .stream()
                .filter(candidate -> candidate.id().equals(candidateId))
                .findFirst()
                .orElseThrow();
    }

    private CandidateIds persistNewCandidate() {
        return persistNewCandidate(LocalDateTime.of(2026, 8, 1, 9, 0));
    }

    private CandidateIds persistNewCandidate(LocalDateTime publishedAt) {
        return transaction().execute(status -> {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Company company = Company.create("company-" + suffix, "Company " + suffix);
            entityManager.persist(company);
            BlogSource source = BlogSource.create(
                    company,
                    "source-" + suffix,
                    "Source " + suffix,
                    "https://example.com/" + suffix,
                    "https://example.com/" + suffix + "/feed",
                    CollectionMethod.RSS
            );
            entityManager.persist(source);
            String hash = "hash-" + suffix;
            ArticleCandidateTask candidate = ArticleCandidateTask.create(
                    company,
                    source,
                    new ArticleCandidate(
                            company.getCompanyKey(),
                            company.getCompanyName(),
                            "Original title",
                            "https://example.com/articles/" + suffix,
                            publishedAt,
                            "Short context",
                            hash,
                            false,
                            ArticleCandidateDecisionStatus.NEW,
                            List.of()
                    ),
                    ArticleCandidateDecisionStatus.NEW,
                    "v1"
            );
            entityManager.persist(candidate);
            entityManager.flush();
            return new CandidateIds(company.getId(), candidate.getId(), hash);
        });
    }

    private ArticleCandidateTask candidate(Long candidateId) {
        return candidateRepository.findById(candidateId).orElseThrow();
    }

    private ArticleMetadataDecision approvedDecision(String translatedTitle) {
        return new ArticleMetadataDecision(
                0,
                translatedTitle,
                ArticleCategory.AI,
                true,
                null
        );
    }

    private TransactionTemplate transaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private record CandidateIds(Long companyId, Long candidateId, String hash) {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        AiReviewProperties aiReviewProperties() {
            return new AiReviewProperties(
                    100,
                    10,
                    3,
                    100,
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(15)
            );
        }
    }
}
