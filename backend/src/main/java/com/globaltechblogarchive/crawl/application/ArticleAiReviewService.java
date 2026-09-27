package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.application.ArticleMetadataAiClient;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiRequestException;
import com.globaltechblogarchive.crawl.application.dto.AiReviewCandidateResult;
import com.globaltechblogarchive.crawl.application.dto.AiReviewRunResult;
import com.globaltechblogarchive.crawl.application.dto.ClaimedArticleCandidate;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleAiReviewService {

    static final String PROMPT_VERSION = "v1";

    private final ArticleMetadataAiClient aiClient;
    private final ArticleCandidateStateService stateService;
    private final ArticleAiReviewResultService resultService;
    private final AiReviewProperties properties;

    public AiReviewRunResult runScheduled() {
        LocalDateTime now = LocalDateTime.now();
        int recoveredCount = stateService.recoverStale(now);
        List<ClaimedArticleCandidate> candidates = stateService.claimAvailable(
                now,
                PROMPT_VERSION,
                properties.claimLimit()
        );
        return process(candidates, recoveredCount);
    }

    public AiReviewRunResult retryFailed(int limit) {
        List<ClaimedArticleCandidate> candidates = stateService.claimFailed(
                LocalDateTime.now(),
                PROMPT_VERSION,
                limit
        );
        return process(candidates, 0);
    }

    private AiReviewRunResult process(
            List<ClaimedArticleCandidate> candidates,
            int recoveredCount
    ) {
        AiReviewRunResult result = AiReviewRunResult.empty(recoveredCount);
        for (int start = 0; start < candidates.size(); start += properties.batchSize()) {
            List<ClaimedArticleCandidate> batch = candidates.subList(
                    start,
                    Math.min(start + properties.batchSize(), candidates.size())
            );
            result = result.plus(processBatch(batch));
        }
        return result;
    }

    private AiReviewRunResult processBatch(List<ClaimedArticleCandidate> batch) {
        List<ArticleMetadataDecision> decisions;
        try {
            decisions = aiClient.decide(toInputs(batch));
        } catch (ArticleMetadataAiRequestException exception) {
            return failBatch(
                    batch,
                    exception.isRetryable(),
                    exception.getFailureCode(),
                    exception.getMessage()
            );
        } catch (ArticleMetadataAiClientException exception) {
            return failBatch(batch, false, "OPENAI_RESPONSE_INVALID", exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Unexpected AI review failure: batchSize={}", batch.size(), exception);
            return failBatch(batch, false, "AI_REVIEW_UNEXPECTED", exception.getMessage());
        }

        Map<Integer, ArticleMetadataDecision> decisionsByIndex = decisionsByIndex(decisions, batch.size());
        if (decisionsByIndex == null) {
            return failBatch(batch, false, "OPENAI_RESPONSE_INVALID", "Invalid AI decision indexes");
        }

        AiReviewRunResult result = AiReviewRunResult.empty(0);
        for (int index = 0; index < batch.size(); index++) {
            ClaimedArticleCandidate candidate = batch.get(index);
            ArticleMetadataDecision decision = decisionsByIndex.get(index);
            AiReviewCandidateResult candidateResult;
            if (decision == null) {
                candidateResult = resultService.fail(
                        candidate,
                        LocalDateTime.now(),
                        false,
                        "OPENAI_RESPONSE_MISSING",
                        "AI decision is missing for candidate index " + index
                );
            } else {
                candidateResult = complete(candidate, decision);
            }
            result = result.plus(candidateResult.toRunResult());
        }
        return result;
    }

    private AiReviewCandidateResult complete(
            ClaimedArticleCandidate candidate,
            ArticleMetadataDecision decision
    ) {
        try {
            return resultService.complete(candidate, decision, aiClient.model());
        } catch (RuntimeException exception) {
            log.error("AI review result persistence failed: candidateId={}", candidate.id(), exception);
            return resultService.fail(
                    candidate,
                    LocalDateTime.now(),
                    true,
                    "AI_RESULT_SAVE_ERROR",
                    exception.getMessage()
            );
        }
    }

    private AiReviewRunResult failBatch(
            List<ClaimedArticleCandidate> batch,
            boolean retryable,
            String errorCode,
            String errorMessage
    ) {
        AiReviewRunResult result = AiReviewRunResult.empty(0);
        LocalDateTime failedAt = LocalDateTime.now();
        for (ClaimedArticleCandidate candidate : batch) {
            AiReviewCandidateResult candidateResult = resultService.fail(
                    candidate,
                    failedAt,
                    retryable,
                    errorCode,
                    errorMessage
            );
            result = result.plus(candidateResult.toRunResult());
        }
        return result;
    }

    private List<ArticleMetadataInput> toInputs(List<ClaimedArticleCandidate> batch) {
        return IntStream.range(0, batch.size())
                .mapToObj(index -> {
                    ClaimedArticleCandidate candidate = batch.get(index);
                    return new ArticleMetadataInput(
                            index,
                            candidate.originalTitle(),
                            candidate.shortContext(),
                            candidate.categoryHint()
                    );
                })
                .toList();
    }

    private Map<Integer, ArticleMetadataDecision> decisionsByIndex(
            List<ArticleMetadataDecision> decisions,
            int batchSize
    ) {
        Map<Integer, ArticleMetadataDecision> result = new HashMap<>();
        for (ArticleMetadataDecision decision : decisions) {
            if (decision.index() < 0 || decision.index() >= batchSize) {
                return null;
            }
            if (result.put(decision.index(), decision) != null) {
                return null;
            }
        }
        return result;
    }
}
