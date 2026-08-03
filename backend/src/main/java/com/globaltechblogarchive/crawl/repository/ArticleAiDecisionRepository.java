package com.globaltechblogarchive.crawl.repository;

import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleAiDecisionRepository extends JpaRepository<ArticleAiDecision, Long> {

    boolean existsByCompanyIdAndArticleUrlHashAndPromptVersion(
            Long companyId,
            String articleUrlHash,
            String promptVersion
    );

    List<ArticleAiDecision> findByCompanyIdAndArticleUrlHashInAndPromptVersion(
            Long companyId,
            Collection<String> articleUrlHashes,
            String promptVersion
    );

    @EntityGraph(attributePaths = "company")
    List<ArticleAiDecision> findAllByOrderByIdAsc();
}
