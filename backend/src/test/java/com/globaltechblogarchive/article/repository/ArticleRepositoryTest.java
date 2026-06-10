package com.globaltechblogarchive.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.domain.CollectionMethod;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findAllByOrderByPublishedAtDescIdDescReturnsLatestArticlesFirst() {
        BlogSource source = persistSource();
        Article older = persistArticle(source, "older", ArticleCategory.FRONTEND, LocalDateTime.of(2026, 6, 1, 10, 0));
        Article newer = persistArticle(source, "newer", ArticleCategory.BACKEND, LocalDateTime.of(2026, 6, 2, 10, 0));
        entityManager.flush();
        entityManager.clear();

        Page<Article> page = articleRepository.findAllByOrderByPublishedAtDescIdDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Article::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(page.getContent().getFirst().getCompany().getCompanyName()).isEqualTo("OpenAI");
    }

    @Test
    void findByCategoryOrderByPublishedAtDescIdDescReturnsOnlyMatchingCategory() {
        BlogSource source = persistSource();
        Article backend = persistArticle(source, "backend", ArticleCategory.BACKEND, LocalDateTime.of(2026, 6, 1, 10, 0));
        persistArticle(source, "frontend", ArticleCategory.FRONTEND, LocalDateTime.of(2026, 6, 2, 10, 0));
        entityManager.flush();
        entityManager.clear();

        Page<Article> page = articleRepository.findByCategoryOrderByPublishedAtDescIdDesc(
                ArticleCategory.BACKEND,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).extracting(Article::getId)
                .containsExactly(backend.getId());
    }

    @Test
    void existsByCompanyIdAndArticleUrlHashReturnsWhetherArticleWasStored() {
        BlogSource source = persistSource();
        persistArticle(source, "existing", ArticleCategory.AI, LocalDateTime.of(2026, 6, 1, 10, 0));
        entityManager.flush();
        entityManager.clear();

        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(source.getCompany().getId(), "hash-existing")).isTrue();
        assertThat(articleRepository.existsByCompanyIdAndArticleUrlHash(source.getCompany().getId(), "hash-missing")).isFalse();
    }

    private BlogSource persistSource() {
        Company company = Company.create("test-openai", "OpenAI");
        entityManager.persist(company);
        BlogSource source = BlogSource.create(
                company,
                "test-openai",
                "OpenAI News",
                "https://openai.com/news/",
                "https://openai.com/news/rss.xml",
                CollectionMethod.RSS
        );
        entityManager.persist(source);
        return source;
    }

    private Article persistArticle(
            BlogSource source,
            String slug,
            ArticleCategory category,
            LocalDateTime publishedAt
    ) {
        Article article = Article.create(
                source.getCompany(),
                "Article " + slug,
                "https://openai.com/news/" + slug,
                "hash-" + slug,
                category,
                publishedAt
        );
        entityManager.persist(article);
        return article;
    }
}
