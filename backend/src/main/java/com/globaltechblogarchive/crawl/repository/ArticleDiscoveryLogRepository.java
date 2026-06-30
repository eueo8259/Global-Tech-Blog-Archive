package com.globaltechblogarchive.crawl.repository;

import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleDiscoveryLogRepository extends JpaRepository<ArticleDiscoveryLog, Long> {

    @EntityGraph(attributePaths = {"source", "source.company"})
    @Query("""
            SELECT log
            FROM ArticleDiscoveryLog log
            WHERE log.decisionStatus = :status
              AND log.id = (
                  SELECT MIN(previous.id)
                  FROM ArticleDiscoveryLog previous
                  WHERE previous.decisionStatus = :status
                    AND previous.source.company.id = log.source.company.id
                    AND previous.articleUrlHash = log.articleUrlHash
              )
              AND NOT EXISTS (
                  SELECT decision.id
                  FROM ArticleAiDecision decision
                  WHERE decision.company.id = log.source.company.id
                    AND decision.articleUrlHash = log.articleUrlHash
                    AND decision.promptVersion = :promptVersion
              )
              AND NOT EXISTS (
                  SELECT article.id
                  FROM Article article
                  WHERE article.company.id = log.source.company.id
                    AND article.articleUrlHash = log.articleUrlHash
              )
            ORDER BY log.createdAt, log.id
            """)
    List<ArticleDiscoveryLog> findUnresolvedAiFailures(
            @Param("status") ArticleCandidateDecisionStatus status,
            @Param("promptVersion") String promptVersion,
            Pageable pageable
    );
}
