package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.application.ArticleService;
import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleDecisionProcessor {

    private static final int AI_BATCH_SIZE = 10;

    private final ArticleAiDecisionRepository decisionRepository;
    private final ArticleMetadataAiClient aiClient;
    private final ArticleService articleService;

    public ProcessedCandidates process(
            BlogSource source,
            List<ArticleCandidate> sourceCandidates,
            Map<String, ArticleAiDecision> decisionsByHash,
            String promptVersion
    ) {
        List<ArticleCandidate> candidates = new ArrayList<>(sourceCandidates);
        List<ArticleCandidate> newCandidates = aiTargetCandidates(candidates);

        for (int start = 0; start < newCandidates.size(); start += AI_BATCH_SIZE) {
            List<ArticleCandidate> batch = newCandidates.subList(
                    start,
                    Math.min(start + AI_BATCH_SIZE, newCandidates.size())
            );
            List<ArticleMetadataDecision> aiDecisions;
            try {
                aiDecisions = aiClient.decide(toAiInputs(batch));
            } catch (RuntimeException exception) {
                markBatchFailed(candidates, batch);
                continue;
            }
            applyAiDecisions(source, candidates, batch, decisionsByHash, aiDecisions, promptVersion);
        }

        int storedArticleCount = writeApproved(source, candidates, decisionsByHash);
        return new ProcessedCandidates(candidates, storedArticleCount);
    }

    private int writeApproved(
            BlogSource source,
            List<ArticleCandidate> candidates,
            Map<String, ArticleAiDecision> decisionsByHash
    ) {
        int storedArticleCount = 0;
        for (ArticleCandidate candidate : candidates) {
            ArticleAiDecision decision = decisionsByHash.get(candidate.articleUrlHash());
            if (candidate.duplicate()
                    || decision == null
                    || !isSaveTarget(decision.isSaveTarget(), decision.getCategory())) {
                continue;
            }
            articleService.save(Article.create(
                    source.getCompany(),
                    decision.getTranslatedTitle(),
                    candidate.articleUrl(),
                    candidate.articleUrlHash(),
                    decision.getCategory(),
                    publishedAt(candidate)
            ));
            storedArticleCount++;
        }
        return storedArticleCount;
    }

    private List<ArticleCandidate> aiTargetCandidates(List<ArticleCandidate> candidates) {
        return candidates.stream()
                .filter(candidate -> candidate.decisionStatus() == ArticleCandidateDecisionStatus.NEW)
                .filter(candidate -> !candidate.duplicate())
                .toList();
    }

    private List<ArticleMetadataInput> toAiInputs(List<ArticleCandidate> candidates) {
        List<ArticleMetadataInput> inputs = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            inputs.add(new ArticleMetadataInput(index, candidates.get(index).originalTitle()));
        }
        return inputs;
    }

    private void applyAiDecisions(
            BlogSource source,
            List<ArticleCandidate> candidates,
            List<ArticleCandidate> batch,
            Map<String, ArticleAiDecision> decisionsByHash,
            List<ArticleMetadataDecision> aiDecisions,
            String promptVersion
    ) {
        Map<Integer, ArticleMetadataDecision> decisionsByIndex = aiDecisions.stream()
                .collect(Collectors.toMap(
                        ArticleMetadataDecision::index,
                        Function.identity()
                ));

        for (int index = 0; index < batch.size(); index++) {
            ArticleCandidate candidate = batch.get(index);
            ArticleMetadataDecision metadataDecision = decisionsByIndex.get(index);
            int candidateIndex = candidates.indexOf(candidate);
            if (metadataDecision == null) {
                candidates.set(candidateIndex, candidate.withDecisionStatus(ArticleCandidateDecisionStatus.AI_FAILED));
                continue;
            }

            boolean saveTarget = isSaveTarget(metadataDecision.save(), metadataDecision.category());
            ArticleAiDecision decision = ArticleAiDecision.create(
                    source.getCompany(),
                    candidate.articleUrlHash(),
                    candidate.articleUrl(),
                    candidate.originalTitle(),
                    metadataDecision.translatedTitle(),
                    metadataDecision.category(),
                    saveTarget,
                    aiClient.model(),
                    promptVersion
            );
            decisionRepository.save(decision);
            decisionsByHash.put(candidate.articleUrlHash(), decision);
            if (saveTarget) {
                candidates.set(candidateIndex, candidate.withDecisionStatus(ArticleCandidateDecisionStatus.AI_APPROVED));
            } else {
                candidates.set(candidateIndex, candidate.withDecisionStatus(ArticleCandidateDecisionStatus.AI_REJECTED));
            }
        }
    }

    private void markBatchFailed(List<ArticleCandidate> candidates, List<ArticleCandidate> batch) {
        for (ArticleCandidate candidate : batch) {
            int candidateIndex = candidates.indexOf(candidate);
            candidates.set(candidateIndex, candidate.withDecisionStatus(ArticleCandidateDecisionStatus.AI_FAILED));
        }
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

    public record ProcessedCandidates(
            List<ArticleCandidate> candidates,
            int storedArticleCount
    ) {
    }
}
