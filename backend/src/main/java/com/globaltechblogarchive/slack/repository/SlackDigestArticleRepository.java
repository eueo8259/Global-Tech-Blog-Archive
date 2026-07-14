package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.article.domain.Article;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SlackDigestArticleRepository extends Repository<Article, Long> {

    @Query("""
            select article.id as articleId,
                   company.id as companyId,
                   company.companyName as companyName,
                   article.title as title,
                   article.articleUrl as articleUrl,
                   article.publishedAt as publishedAt
            from Article article
            join article.company company
            where article.createdAt > :windowStartedAt
              and article.createdAt <= :windowEndedAt
              and exists (
                    select subscription.id
                    from SlackChannelSubscription subscription
                    where subscription.slackChannel.id = :channelId
                      and subscription.company = company
                      and subscription.createdAt <= article.createdAt
              )
            order by company.companyName, article.publishedAt, article.id
            """)
    List<SlackDigestArticleProjection> findDigestArticles(
            @Param("channelId") Long channelId,
            @Param("windowStartedAt") LocalDateTime windowStartedAt,
            @Param("windowEndedAt") LocalDateTime windowEndedAt
    );

    @Query("""
            select count(article) > 0
            from Article article
            where article.createdAt > :windowStartedAt
              and article.createdAt <= :windowEndedAt
              and exists (
                    select subscription.id
                    from SlackChannelSubscription subscription
                    where subscription.slackChannel.id = :channelId
                      and subscription.company = article.company
                      and subscription.createdAt <= article.createdAt
              )
            """)
    boolean existsDigestArticles(
            @Param("channelId") Long channelId,
            @Param("windowStartedAt") LocalDateTime windowStartedAt,
            @Param("windowEndedAt") LocalDateTime windowEndedAt
    );
}
