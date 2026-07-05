package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates;
import com.globaltechblogarchive.crawl.application.CrawlPersistenceService;
import com.globaltechblogarchive.crawl.application.PreparedArticleDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleAiDecisionRepository decisionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void persistCollectionRollsBackDecisionsAndArticlesWhenLogSaveFails() {
        PersistedIds ids = persistRunAndSource();
        long articleCount = articleRepository.count();
        long decisionCount = decisionRepository.count();
        PreparedArticleDecision decision = new PreparedArticleDecision(
                "rollback-hash",
                "https://example.com/rollback",
                "Original",
                "Translated",
                ArticleCategory.BACKEND,
                true,
                "test-model",
                "v1"
        );
        ArticleCandidate invalidLogCandidate = new ArticleCandidate(
                "rollback-company",
                "Rollback Company",
                null,
                decision.articleUrl(),
                LocalDateTime.of(2026, 7, 1, 10, 0),
                "Context",
                decision.articleUrlHash(),
                false,
                ArticleCandidateDecisionStatus.AI_APPROVED,
                List.of()
        );
        ProcessedCandidates processed = new ProcessedCandidates(
                List.of(invalidLogCandidate),
                Map.of(decision.articleUrlHash(), decision),
                List.of(decision)
        );

        assertThatThrownBy(() -> persistenceService.persistCollection(
                ids.runId(),
                ids.sourceId(),
                processed
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(articleRepository.count()).isEqualTo(articleCount);
        assertThat(decisionRepository.count()).isEqualTo(decisionCount);
    }

    private PersistedIds persistRunAndSource() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> {
            Company company = Company.create("rollback-company", "Rollback Company");
            entityManager.persist(company);
            BlogSource source = BlogSource.create(
                    company,
                    "rollback-source",
                    "Rollback Source",
                    "https://example.com",
                    "https://example.com/feed",
                    CollectionMethod.RSS
            );
            entityManager.persist(source);
            ArticleCollectionRun run = ArticleCollectionRun.start(
                    LocalDateTime.of(2026, 7, 1, 9, 0)
            );
            entityManager.persist(run);
            entityManager.flush();
            return new PersistedIds(run.getId(), source.getId());
        });
    }

    private record PersistedIds(Long runId, Long sourceId) {
    }
}
