package com.globaltechblogarchive.slack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.company.domain.Company;
import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackChannelSubscription;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import com.globaltechblogarchive.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class SlackDigestArticleRepositoryTest extends MySqlIntegrationTest {

    @Autowired
    private SlackDigestArticleRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findDigestArticlesReturnsOnlySubscribedArticlesCreatedAfterSubscriptionInWindow() {
        SlackWorkspace workspace = persistWorkspace();
        SlackChannel channel = persistChannel(workspace);
        Company netflix = persistCompany("digest-netflix", "Netflix");
        Company uber = persistCompany("digest-uber", "Uber");
        entityManager.persist(SlackChannelSubscription.create(channel, netflix));
        entityManager.flush();

        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2030, 1, 2, 9, 0);
        persistArticle(netflix, "포함", "included", start.plusHours(1));
        persistArticle(netflix, "시작 경계 제외", "start-boundary", start);
        persistArticle(netflix, "종료 경계 포함", "end-boundary", end);
        persistArticle(uber, "미구독 제외", "unsubscribed", start.plusHours(2));
        persistArticle(netflix, "구독 전 제외", "before-subscription", LocalDateTime.of(2020, 1, 1, 0, 0));
        entityManager.flush();
        entityManager.clear();

        List<SlackDigestArticleProjection> result = repository.findDigestArticles(
                channel.getId(),
                start,
                end
        );

        assertThat(result).extracting(SlackDigestArticleProjection::getTitle)
                .containsExactly("포함", "종료 경계 포함");
        assertThat(repository.existsDigestArticles(channel.getId(), start, end)).isTrue();
    }

    private SlackWorkspace persistWorkspace() {
        SlackWorkspace workspace = SlackWorkspace.create(
                "T-DIGEST",
                "Digest Workspace",
                "encrypted-token",
                "B-DIGEST",
                "commands",
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        entityManager.persist(workspace);
        return workspace;
    }

    private SlackChannel persistChannel(SlackWorkspace workspace) {
        SlackChannel channel = SlackChannel.create(workspace, "C-DIGEST", "digest");
        entityManager.persist(channel);
        return channel;
    }

    private Company persistCompany(String key, String name) {
        Company company = Company.create(key, name);
        entityManager.persist(company);
        return company;
    }

    private void persistArticle(
            Company company,
            String title,
            String hashSeed,
            LocalDateTime createdAt
    ) {
        Article article = Article.restore(
                company,
                title,
                "https://example.com/" + hashSeed,
                String.format("%064d", Math.abs(hashSeed.hashCode())),
                ArticleCategory.BACKEND,
                createdAt,
                createdAt,
                createdAt
        );
        entityManager.persist(article);
    }
}
