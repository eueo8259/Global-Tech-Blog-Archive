package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.application.CrawlPersistenceService;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.repository.ArticleCandidateTaskRepository;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import(CrawlPersistenceService.class)
class CrawlPersistenceServiceTest extends MySqlIntegrationTest {

    @Autowired
    private CrawlPersistenceService persistenceService;

    @Autowired
    private ArticleCandidateTaskRepository candidateRepository;

    @Autowired
    private ArticleDiscoveryLogRepository discoveryLogRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void collectedCandidateIsStoredAsNewBeforeAiProcessing() {
        PersistedIds ids = persistRunAndSource();
        ArticleCandidate candidate = candidate("hash-new", ArticleCandidateDecisionStatus.NEW, false);

        persistenceService.persistDiscoveredCandidates(
                ids.runId(),
                ids.sourceId(),
                List.of(candidate),
                Map.of()
        );

        assertThat(candidateRepository.findByCompanyIdAndArticleUrlHash(ids.companyId(), "hash-new"))
                .get()
                .satisfies(task -> {
                    assertThat(task.getStatus()).isEqualTo(ArticleCandidateDecisionStatus.NEW);
                    assertThat(task.getAttemptCount()).isZero();
                });
        assertThat(discoveryLogRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rediscoveredUrlUpdatesSingleCandidateAndKeepsDiscoveryHistory() {
        PersistedIds ids = persistRunAndSource();
        ArticleCandidate candidate = candidate("same-hash", ArticleCandidateDecisionStatus.NEW, false);
        persistenceService.persistDiscoveredCandidates(
                ids.runId(),
                ids.sourceId(),
                List.of(candidate),
                Map.of()
        );
        Long firstCandidateId = candidateRepository
                .findByCompanyIdAndArticleUrlHash(ids.companyId(), "same-hash")
                .orElseThrow()
                .getId();
        Long secondRunId = persistRun();

        persistenceService.persistDiscoveredCandidates(
                secondRunId,
                ids.sourceId(),
                List.of(candidate),
                Map.of()
        );

        assertThat(candidateRepository.findByCompanyIdAndArticleUrlHash(ids.companyId(), "same-hash"))
                .get()
                .extracting(task -> task.getId())
                .isEqualTo(firstCandidateId);
        assertThat(discoveryLogRepository.countBySourceIdAndArticleUrlHash(ids.sourceId(), "same-hash"))
                .isEqualTo(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void duplicateHashInSameCollectionIsStoredOnce() {
        PersistedIds ids = persistRunAndSource();
        ArticleCandidate first = candidate("same-run-hash", ArticleCandidateDecisionStatus.NEW, false);
        ArticleCandidate duplicate = candidate("same-run-hash", ArticleCandidateDecisionStatus.NEW, false);

        SourceCrawlResult result = persistenceService.persistDiscoveredCandidates(
                ids.runId(),
                ids.sourceId(),
                List.of(first, duplicate),
                Map.of()
        );

        assertThat(result.candidates()).hasSize(1);
        assertThat(candidateRepository.findByCompanyIdAndArticleUrlHash(ids.companyId(), "same-run-hash"))
                .isPresent();
        assertThat(discoveryLogRepository.countBySourceIdAndArticleUrlHash(ids.sourceId(), "same-run-hash"))
                .isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void existingArticleIsRecordedAsDuplicateAndNeverQueuedForAi() {
        PersistedIds ids = persistRunAndSource();
        ArticleCandidate duplicate = candidate("duplicate-hash", ArticleCandidateDecisionStatus.NEW, true);

        persistenceService.persistDiscoveredCandidates(
                ids.runId(),
                ids.sourceId(),
                List.of(duplicate),
                Map.of()
        );

        assertThat(candidateRepository.findByCompanyIdAndArticleUrlHash(ids.companyId(), "duplicate-hash"))
                .get()
                .extracting(task -> task.getStatus())
                .isEqualTo(ArticleCandidateDecisionStatus.DUPLICATE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void previousDecisionReuseStoresOnlyPreviouslyApprovedArticle() {
        PersistedIds ids = persistRunAndSource();
        ArticleCandidate approved = candidate(
                "approved-hash",
                ArticleCandidateDecisionStatus.PREVIOUSLY_APPROVED,
                false
        );
        ArticleCandidate rejected = candidate(
                "rejected-hash",
                ArticleCandidateDecisionStatus.PREVIOUSLY_REJECTED,
                false
        );
        Company detachedCompany = Company.create("detached", "Detached");
        ArticleAiDecision approvedDecision = decision(
                detachedCompany,
                approved,
                true,
                ArticleCategory.AI
        );
        ArticleAiDecision rejectedDecision = decision(
                detachedCompany,
                rejected,
                false,
                ArticleCategory.ELSE
        );

        persistenceService.persistDiscoveredCandidates(
                ids.runId(),
                ids.sourceId(),
                List.of(approved, rejected),
                Map.of(
                        approved.articleUrlHash(), approvedDecision,
                        rejected.articleUrlHash(), rejectedDecision
                )
        );

        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(ids.companyId(), "approved-hash"))
                .isTrue();
        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(ids.companyId(), "rejected-hash"))
                .isFalse();
    }

    private PersistedIds persistRunAndSource() {
        TransactionTemplate transaction = transaction();
        return transaction.execute(status -> {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Company company = Company.create("queue-company-" + suffix, "Queue Company");
            entityManager.persist(company);
            BlogSource source = BlogSource.create(
                    company,
                    "queue-source-" + suffix,
                    "Queue Source",
                    "https://example.com",
                    "https://example.com/feed",
                    CollectionMethod.RSS
            );
            entityManager.persist(source);
            ArticleCollectionRun run = ArticleCollectionRun.start(LocalDateTime.now());
            entityManager.persist(run);
            entityManager.flush();
            return new PersistedIds(company.getId(), source.getId(), run.getId());
        });
    }

    private Long persistRun() {
        return transaction().execute(status -> {
            ArticleCollectionRun run = ArticleCollectionRun.start(LocalDateTime.now());
            entityManager.persist(run);
            entityManager.flush();
            return run.getId();
        });
    }

    private TransactionTemplate transaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private ArticleCandidate candidate(
            String hash,
            ArticleCandidateDecisionStatus status,
            boolean duplicate
    ) {
        return new ArticleCandidate(
                "queue-company",
                "Queue Company",
                "Original title",
                "https://example.com/" + hash,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                "Short context",
                hash,
                duplicate,
                status,
                List.of()
        );
    }

    private ArticleAiDecision decision(
            Company company,
            ArticleCandidate candidate,
            boolean saveTarget,
            ArticleCategory category
    ) {
        return ArticleAiDecision.create(
                company,
                candidate.articleUrlHash(),
                candidate.articleUrl(),
                candidate.originalTitle(),
                "Translated title",
                category,
                saveTarget,
                "gpt-test",
                "v1"
        );
    }

    private record PersistedIds(Long companyId, Long sourceId, Long runId) {
    }
}
