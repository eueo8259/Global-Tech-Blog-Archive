package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleDecisionProcessor {

    private static final int AI_BATCH_SIZE = 10;

    private final ArticleMetadataAiClient aiClient;

    public ProcessedCandidates process(
            BlogSource source,
            List<ArticleCandidate> sourceCandidates,
            Map<String, PreparedArticleDecision> decisionsByHash,
            String promptVersion
    ) {
        List<ArticleCandidate> candidates = new ArrayList<>(sourceCandidates);
        List<PreparedArticleDecision> newDecisions = new ArrayList<>();
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
                log.error(
                        "AI article metadata decision failed: sourceKey={}, batchSize={}",
                        source.getSourceKey(),
                        batch.size(),
                        exception
                );
                markBatchFailed(candidates, batch);
                continue;
            }
            applyAiDecisions(
                    candidates,
                    batch,
                    decisionsByHash,
                    newDecisions,
                    aiDecisions,
                    promptVersion
            );
        }

        return new ProcessedCandidates(candidates, decisionsByHash, newDecisions);
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
            ArticleCandidate candidate = candidates.get(index);
            inputs.add(new ArticleMetadataInput(
                    index,
                    candidate.originalTitle(),
                    candidate.shortContext(),
                    candidate.categoryHint()
            ));
        }
        return inputs;
    }

    private void applyAiDecisions(
            List<ArticleCandidate> candidates,
            List<ArticleCandidate> batch,
            Map<String, PreparedArticleDecision> decisionsByHash,
            List<PreparedArticleDecision> newDecisions,
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
            PreparedArticleDecision decision = new PreparedArticleDecision(
                    candidate.articleUrlHash(),
                    candidate.articleUrl(),
                    candidate.originalTitle(),
                    metadataDecision.translatedTitle(),
                    metadataDecision.category(),
                    saveTarget,
                    aiClient.model(),
                    promptVersion
            );
            decisionsByHash.put(candidate.articleUrlHash(), decision);
            newDecisions.add(decision);
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

    public record ProcessedCandidates(
            List<ArticleCandidate> candidates,
            Map<String, PreparedArticleDecision> decisionsByHash,
            List<PreparedArticleDecision> newDecisions
    ) {
    }
}
