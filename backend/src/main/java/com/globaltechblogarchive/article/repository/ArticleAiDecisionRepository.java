package com.globaltechblogarchive.article.repository;

import com.globaltechblogarchive.article.domain.ArticleAiDecision;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleAiDecisionRepository extends JpaRepository<ArticleAiDecision, Long> {

    List<ArticleAiDecision> findBySourceIdAndNormalizedUrlHashInAndPromptVersion(
            Long sourceId,
            Collection<String> normalizedUrlHashes,
            String promptVersion
    );
}
