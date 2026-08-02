package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.ClaimedArticleCandidate;
import com.globaltechblogarchive.crawl.config.AiReviewProperties;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateTask;
import com.globaltechblogarchive.crawl.repository.ArticleCandidateTaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleCandidateStateService {

    private final ArticleCandidateTaskRepository candidateRepository;
    private final AiReviewProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedArticleCandidate> claimAvailable(
            LocalDateTime now,
            String promptVersion,
            int limit
    ) {
        List<ArticleCandidateTask> candidates = candidateRepository.findClaimable(
                ArticleCandidateDecisionStatus.NEW,
                ArticleCandidateDecisionStatus.AI_RETRY_WAITING,
                now,
                PageRequest.of(0, limit)
        );
        return claim(candidates, now, promptVersion);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedArticleCandidate> claimFailed(
            LocalDateTime now,
            String promptVersion,
            int limit
    ) {
        List<ArticleCandidateTask> candidates = candidateRepository.findByStatusOrderByUpdatedAtAscIdAsc(
                ArticleCandidateDecisionStatus.AI_FAILED,
                PageRequest.of(0, limit)
        );
        return claim(candidates, now, promptVersion);
    }

    @Transactional
    public int recoverStale(LocalDateTime now) {
        LocalDateTime threshold = now.minus(properties.staleTimeout());
        List<ArticleCandidateTask> candidates = candidateRepository.findStaleProcessing(
                ArticleCandidateDecisionStatus.AI_PROCESSING,
                threshold
        );
        candidates.forEach(candidate -> candidate.recover(now));
        return candidates.size();
    }

    private List<ClaimedArticleCandidate> claim(
            List<ArticleCandidateTask> candidates,
            LocalDateTime now,
            String promptVersion
    ) {
        return candidates.stream()
                .map(candidate -> {
                    candidate.claim(now, promptVersion);
                    return toClaimed(candidate, now, promptVersion);
                })
                .toList();
    }

    private ClaimedArticleCandidate toClaimed(
            ArticleCandidateTask candidate,
            LocalDateTime claimedAt,
            String promptVersion
    ) {
        return new ClaimedArticleCandidate(
                candidate.getId(),
                candidate.getCompany().getId(),
                candidate.getSource().getId(),
                candidate.getArticleUrl(),
                candidate.getArticleUrlHash(),
                candidate.getOriginalTitle(),
                candidate.getShortContext(),
                candidate.getCategoryHint(),
                candidate.getPublishedAt(),
                candidate.getAttemptCount(),
                promptVersion,
                claimedAt
        );
    }
}
