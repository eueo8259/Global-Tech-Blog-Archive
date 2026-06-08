package com.globaltechblogarchive.article.repository;

import com.globaltechblogarchive.article.domain.ArticleAiDecision;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleAiDecisionRepository extends JpaRepository<ArticleAiDecision, Long> {

    List<ArticleAiDecision> findByCompanyIdAndNormalizedUrlHashInAndPromptVersion(
            Long companyId,
            Collection<String> normalizedUrlHashes,
            String promptVersion
    );
}
