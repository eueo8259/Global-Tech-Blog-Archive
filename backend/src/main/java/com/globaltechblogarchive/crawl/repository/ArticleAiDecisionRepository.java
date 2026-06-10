package com.globaltechblogarchive.crawl.repository;

import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleAiDecisionRepository extends JpaRepository<ArticleAiDecision, Long> {

    List<ArticleAiDecision> findByCompanyIdAndArticleUrlHashInAndPromptVersion(
            Long companyId,
            Collection<String> articleUrlHashes,
            String promptVersion
    );
}
