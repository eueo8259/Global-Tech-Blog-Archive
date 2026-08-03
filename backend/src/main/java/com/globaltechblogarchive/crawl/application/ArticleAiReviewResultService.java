package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.application.dto.AiReviewCandidateResult;
import com.globaltechblogarchive.crawl.application.dto.ClaimedArticleCandidate;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateTask;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCandidateTaskRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleAiReviewResultService {

    private final ArticleCandidateTaskRepository candidateRepository;
    private final ArticleAiDecisionRepository decisionRepository;
    private final ArticleRepository articleRepository;
    private final AiReviewProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiReviewCandidateResult complete(
            ClaimedArticleCandidate claimed,
            ArticleMetadataDecision metadataDecision,
            String model
    ) {
        ArticleCandidateTask candidate = currentClaim(claimed);
        if (candidate == null) {
            return AiReviewCandidateResult.skipped();
        }

        boolean saveTarget = metadataDecision.save()
                && metadataDecision.category() != ArticleCategory.ELSE;
        saveDecision(candidate, claimed, metadataDecision, model, saveTarget);

        if (!saveTarget) {
            candidate.reject();
            return AiReviewCandidateResult.rejected();
        }

        boolean stored = saveArticleIfAbsent(candidate, claimed, metadataDecision);
        candidate.approve();
        return AiReviewCandidateResult.approved(stored);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiReviewCandidateResult fail(
            ClaimedArticleCandidate claimed,
            LocalDateTime failedAt,
            boolean retryable,
            String errorCode,
            String errorMessage
    ) {
        ArticleCandidateTask candidate = currentClaim(claimed);
        if (candidate == null) {
            return AiReviewCandidateResult.skipped();
        }
        if (retryable && candidate.getAttemptCount() < properties.maxAttempts()) {
            candidate.retryAt(failedAt.plus(properties.retryDelay()), errorCode, errorMessage);
            return AiReviewCandidateResult.retryWaiting();
        }
        candidate.fail(errorCode, errorMessage);
        return AiReviewCandidateResult.failed();
    }

    private ArticleCandidateTask currentClaim(ClaimedArticleCandidate claimed) {
        ArticleCandidateTask candidate = candidateRepository.findByIdForUpdate(claimed.id()).orElse(null);
        if (candidate == null || !candidate.matchesClaim(claimed.claimedAt(), claimed.promptVersion())) {
            return null;
        }
        return candidate;
    }

    private void saveDecision(
            ArticleCandidateTask candidate,
            ClaimedArticleCandidate claimed,
            ArticleMetadataDecision metadataDecision,
            String model,
            boolean saveTarget
    ) {
        boolean exists = decisionRepository.existsByCompanyIdAndArticleUrlHashAndPromptVersion(
                claimed.companyId(),
                claimed.articleUrlHash(),
                claimed.promptVersion()
        );
        if (exists) {
            return;
        }
        decisionRepository.save(ArticleAiDecision.create(
                candidate.getCompany(),
                claimed.articleUrlHash(),
                claimed.articleUrl(),
                claimed.originalTitle(),
                metadataDecision.translatedTitle(),
                metadataDecision.category(),
                saveTarget,
                model,
                claimed.promptVersion()
        ));
    }

    private boolean saveArticleIfAbsent(
            ArticleCandidateTask candidate,
            ClaimedArticleCandidate claimed,
            ArticleMetadataDecision metadataDecision
    ) {
        if (articleRepository.existsByCompanyIdAndArticleUrlHash(
                claimed.companyId(),
                claimed.articleUrlHash()
        )) {
            return false;
        }
        LocalDateTime publishedAt = claimed.publishedAt();
        if (publishedAt == null) {
            publishedAt = candidate.getCreatedAt();
        }
        articleRepository.save(Article.create(
                candidate.getCompany(),
                metadataDecision.translatedTitle(),
                claimed.articleUrl(),
                claimed.articleUrlHash(),
                metadataDecision.category(),
                publishedAt
        ));
        return true;
    }
}
