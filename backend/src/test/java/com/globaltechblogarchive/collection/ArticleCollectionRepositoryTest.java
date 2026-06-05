package com.globaltechblogarchive.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.collection.domain.ArticleCandidate;
import com.globaltechblogarchive.collection.domain.ArticleCollectionItem;
import com.globaltechblogarchive.collection.domain.ArticleCollectionRun;
import com.globaltechblogarchive.collection.repository.ArticleCollectionItemRepository;
import com.globaltechblogarchive.collection.repository.ArticleCollectionRunRepository;
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

@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleCollectionRepositoryTest {

    @Autowired
    private ArticleCollectionRunRepository runRepository;

    @Autowired
    private ArticleCollectionItemRepository itemRepository;

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
                List.of("PUBLISHED_AT_MISSING")
        );

        itemRepository.save(ArticleCollectionItem.create(run, source, candidate));
        entityManager.flush();
        entityManager.clear();

        assertThat(itemRepository.countByRunId(run.getId())).isEqualTo(1);
    }

    private BlogSource persistSource() {
        BlogSource source = BlogSource.create(
                "test-source",
                "Test Source",
                "https://example.com/blog",
                null,
                CollectionMethod.HTML_SCRAPING
        );
        entityManager.persist(source);
        return source;
    }
}
