package com.globaltechblogarchive.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleCollectionRepositoryTest {

    @Autowired
    private ArticleCollectionRunRepository runRepository;

    @Autowired
    private ArticleDiscoveryLogRepository itemRepository;

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
                "https://example.com/blog/scaling-backend-systems",
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
