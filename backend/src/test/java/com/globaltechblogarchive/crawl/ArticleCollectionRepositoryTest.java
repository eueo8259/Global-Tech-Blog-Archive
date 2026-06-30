package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class ArticleCollectionRepositoryTest extends MySqlIntegrationTest {

    @Autowired
    private ArticleCollectionRunRepository runRepository;

    @Autowired
    private ArticleDiscoveryLogRepository itemRepository;

    @Autowired
    private ArticleAiDecisionRepository decisionRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveCollectionItemsStoresCandidatesForRun() {
        BlogSource source = persistSource();
        ArticleCollectionRun run = runRepository.save(ArticleCollectionRun.start(LocalDateTime.of(2026, 6, 1, 9, 0)));
        ArticleCandidate candidate = new ArticleCandidate(
                "test-source",
                "Test Source",
                "Scaling backend systems",
                "https://example.com/blog/scaling-backend-systems",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                "Backend scaling context",
                "hash",
                false,
                ArticleCandidateDecisionStatus.NEW,
                List.of("PUBLISHED_AT_MISSING")
        );

        itemRepository.save(ArticleDiscoveryLog.create(run, source, candidate));
        entityManager.flush();
        entityManager.clear();

        assertThat(itemRepository.count()).isEqualTo(1);
    }

    @Test
    void findUnresolvedAiFailuresFiltersBeforeApplyingLimit() {
        BlogSource source = persistSource();
        ArticleCollectionRun run = runRepository.save(ArticleCollectionRun.start(LocalDateTime.of(2026, 6, 1, 9, 0)));
        saveFailure(run, source, "resolved-decision");
        saveFailure(run, source, "stored-article");
        saveFailure(run, source, "retry-first");
        saveFailure(run, source, "retry-first");
        saveFailure(run, source, "retry-second");
        decisionRepository.save(ArticleAiDecision.create(
                source.getCompany(),
                "resolved-decision",
                "https://example.com/resolved-decision",
                "Resolved",
                "Resolved",
                ArticleCategory.ELSE,
                false,
                "test-model",
                "v1"
        ));
        articleRepository.save(Article.create(
                source.getCompany(),
                "Stored",
                "https://example.com/stored-article",
                "stored-article",
                ArticleCategory.BACKEND,
                LocalDateTime.of(2026, 6, 1, 10, 0)
        ));
        entityManager.flush();
        entityManager.clear();

        List<ArticleDiscoveryLog> failures = itemRepository.findUnresolvedAiFailures(
                ArticleCandidateDecisionStatus.AI_FAILED,
                "v1",
                PageRequest.of(0, 1)
        );

        assertThat(failures)
                .extracting(ArticleDiscoveryLog::getArticleUrlHash)
                .containsExactly("retry-first");
    }

    private void saveFailure(
            ArticleCollectionRun run,
            BlogSource source,
            String articleUrlHash
    ) {
        ArticleCandidate candidate = new ArticleCandidate(
                source.getCompany().getCompanyKey(),
                source.getCompany().getCompanyName(),
                "Failed " + articleUrlHash,
                "https://example.com/" + articleUrlHash,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                "Context",
                articleUrlHash,
                false,
                ArticleCandidateDecisionStatus.AI_FAILED,
                List.of()
        );
        itemRepository.saveAndFlush(ArticleDiscoveryLog.create(run, source, candidate));
    }

    private BlogSource persistSource() {
        Company company = Company.create("test-source", "Test Source");
        entityManager.persist(company);
        BlogSource source = BlogSource.create(
                company,
                "test-source",
                "Test Source Blog",
                "https://example.com/blog",
                null,
                CollectionMethod.HTML_SCRAPING
        );
        entityManager.persist(source);
        return source;
    }
}
