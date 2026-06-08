package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleAiDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovedArticleWriter {

    private final ArticleRepository articleRepository;

    public int writeApproved(
            BlogSource source,
            List<ArticleCandidate> candidates,
            Map<String, ArticleAiDecision> decisionsByHash
    ) {
        int storedArticleCount = 0;
        for (ArticleCandidate candidate : candidates) {
            ArticleAiDecision decision = decisionsByHash.get(candidate.normalizedUrlHash());
            if (candidate.duplicate()
                    || decision == null
                    || !isSaveTarget(decision.isSaveTarget(), decision.getCategory())) {
                continue;
            }
            articleRepository.save(Article.create(
                    source.getCompany(),
                    decision.getTranslatedTitle(),
                    candidate.originalUrl(),
                    candidate.normalizedUrl(),
                    candidate.normalizedUrlHash(),
                    decision.getCategory(),
                    publishedAt(candidate)
            ));
            storedArticleCount++;
        }
        return storedArticleCount;
    }

    private boolean isSaveTarget(boolean save, ArticleCategory category) {
        return save && category != ArticleCategory.ELSE;
    }

    private LocalDateTime publishedAt(ArticleCandidate candidate) {
        if (candidate.publishedAt() != null) {
            return candidate.publishedAt();
        }
        return LocalDateTime.now();
    }
}
