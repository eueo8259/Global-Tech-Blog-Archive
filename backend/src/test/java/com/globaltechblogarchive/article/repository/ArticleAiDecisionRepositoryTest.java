package com.globaltechblogarchive.article.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.util.List;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleAiDecisionRepositoryTest {

    @Autowired
    private ArticleAiDecisionRepository decisionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveStoresDecisionValues() {
        BlogSource source = persistSource("openai");
        ArticleAiDecision decision = decisionRepository.save(ArticleAiDecision.create(
                source.getCompany(),
                "hash-approved",
                "https://openai.com/news/scaling",
                "Scaling systems",
                "Translated scaling systems",
                ArticleCategory.ARCHITECTURE,
                true,
                "gpt-5-mini",
                "v1"
        ));
        entityManager.flush();
        entityManager.clear();

        ArticleAiDecision found = decisionRepository.findById(decision.getId()).orElseThrow();

        assertThat(found.getCompany().getId()).isEqualTo(source.getCompany().getId());
        assertThat(found.getArticleUrlHash()).isEqualTo("hash-approved");
        assertThat(found.getArticleUrl()).isEqualTo("https://openai.com/news/scaling");
        assertThat(found.getOriginalTitle()).isEqualTo("Scaling systems");
        assertThat(found.getTranslatedTitle()).isEqualTo("Translated scaling systems");
        assertThat(found.getCategory()).isEqualTo(ArticleCategory.ARCHITECTURE);
        assertThat(found.isSaveTarget()).isTrue();
        assertThat(found.getModel()).isEqualTo("gpt-5-mini");
        assertThat(found.getPromptVersion()).isEqualTo("v1");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByCompanyIdAndArticleUrlHashInAndPromptVersionReturnsMatchingDecisions() {
        BlogSource source = persistSource("source-a");
        BlogSource otherSource = persistSource("source-b");
        ArticleAiDecision included = persistDecision(source, "hash-1", "v1", true, ArticleCategory.AI);
        persistDecision(source, "hash-2", "v2", true, ArticleCategory.BACKEND);
        persistDecision(otherSource, "hash-1", "v1", true, ArticleCategory.DEVOPS);
        entityManager.flush();
        entityManager.clear();

        List<ArticleAiDecision> decisions = decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(
                source.getCompany().getId(),
                List.of("hash-1", "hash-2"),
                "v1"
        );

        assertThat(decisions).extracting(ArticleAiDecision::getId)
                .containsExactly(included.getId());
    }

    @Test
    void uniqueConstraintBlocksSameSourceHashAndPromptVersion() {
        BlogSource source = persistSource("duplicate-source");
        persistDecision(source, "same-hash", "v1", false, ArticleCategory.ELSE);
        entityManager.flush();

        ArticleAiDecision duplicate = ArticleAiDecision.create(
                source.getCompany(),
                "same-hash",
                "https://example.com/blog/duplicate",
                "Duplicate",
                "Translated duplicate",
                ArticleCategory.ELSE,
                false,
                "gpt-5-mini",
                "v1"
        );
        assertThatThrownBy(() -> {
            decisionRepository.save(duplicate);
            entityManager.flush();
        })
                .isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    private BlogSource persistSource(String companyKey) {
        String uniqueCompanyKey = companyKey + "-" + UUID.randomUUID().toString().substring(0, 8);
        Company company = Company.create(uniqueCompanyKey, "Test Company " + uniqueCompanyKey);
        entityManager.persist(company);
        BlogSource source = BlogSource.create(
                company,
                uniqueCompanyKey,
                "Test Source " + uniqueCompanyKey,
                "https://example.com/" + uniqueCompanyKey,
                null,
                CollectionMethod.HTML_SCRAPING
        );
        entityManager.persist(source);
        return source;
    }

    private ArticleAiDecision persistDecision(
            BlogSource source,
            String articleUrlHash,
            String promptVersion,
            boolean saveTarget,
            ArticleCategory category
    ) {
        ArticleAiDecision decision = ArticleAiDecision.create(
                source.getCompany(),
                articleUrlHash,
                "https://example.com/blog/" + articleUrlHash,
                "Original " + articleUrlHash,
                "Translated " + articleUrlHash,
                category,
                saveTarget,
                "gpt-5-mini",
                promptVersion
        );
        entityManager.persist(decision);
        return decision;
    }
}
