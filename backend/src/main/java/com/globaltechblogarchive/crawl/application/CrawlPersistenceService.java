package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateTask;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.repository.ArticleCandidateTaskRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlPersistenceService {

    private final ArticleRepository articleRepository;
    private final ArticleCandidateTaskRepository candidateTaskRepository;
    private final ArticleDiscoveryLogRepository collectionItemRepository;
    private final ArticleCollectionRunRepository collectionRunRepository;
    private final BlogSourceRepository blogSourceRepository;

    @Transactional
    public SourceCrawlResult persistDiscoveredCandidates(
            Long runId,
            Long sourceId,
            List<ArticleCandidate> candidates,
            Map<String, ArticleAiDecision> decisionsByHash
    ) {
        ArticleCollectionRun run = collectionRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Collection run not found: " + runId));
        BlogSource source = blogSourceRepository.findWithCompanyById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Blog source not found: " + sourceId));

        List<ArticleCandidate> observedCandidates = observedCandidates(uniqueCandidates(candidates));
        storeCandidateTasks(source, observedCandidates);
        int storedArticleCount = savePreviouslyApprovedArticles(source, observedCandidates, decisionsByHash);
        storeDiscoveryLogs(run, source, observedCandidates);
        source.markCollected(LocalDateTime.now());

        CrawlRunSummary summary = CrawlRunSummary.from(
                observedCandidates,
                storedArticleCount
        );
        return SourceCrawlResult.success(source, observedCandidates, summary);
    }

    private void storeCandidateTasks(
            BlogSource source,
            List<ArticleCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return;
        }
        List<String> hashes = candidates.stream().map(ArticleCandidate::articleUrlHash).toList();
        Map<String, ArticleCandidateTask> existingByHash = candidateTaskRepository
                .findByCompanyIdAndArticleUrlHashIn(source.getCompany().getId(), hashes)
                .stream()
                .collect(Collectors.toMap(ArticleCandidateTask::getArticleUrlHash, Function.identity()));

        List<ArticleCandidateTask> tasks = candidates.stream()
                .map(candidate -> {
                    ArticleCandidateTask existing = existingByHash.get(candidate.articleUrlHash());
                    if (existing == null) {
                        return ArticleCandidateTask.create(
                                source.getCompany(),
                                source,
                                candidate,
                                candidate.decisionStatus(),
                                ArticleAiReviewService.PROMPT_VERSION
                        );
                    }
                    existing.observe(
                            source,
                            candidate,
                            candidate.decisionStatus(),
                            ArticleAiReviewService.PROMPT_VERSION
                    );
                    return existing;
                })
                .toList();
        candidateTaskRepository.saveAll(tasks);
    }

    private int savePreviouslyApprovedArticles(
            BlogSource source,
            List<ArticleCandidate> candidates,
            Map<String, ArticleAiDecision> decisionsByHash
    ) {
        List<Article> articles = candidates.stream()
                .filter(candidate -> !candidate.duplicate())
                .filter(candidate -> candidate.decisionStatus() == ArticleCandidateDecisionStatus.PREVIOUSLY_APPROVED)
                .map(candidate -> toArticle(source, candidate, decisionsByHash.get(candidate.articleUrlHash())))
                .toList();
        if (!articles.isEmpty()) {
            articleRepository.saveAll(articles);
        }
        return articles.size();
    }

    private Article toArticle(
            BlogSource source,
            ArticleCandidate candidate,
            ArticleAiDecision decision
    ) {
        LocalDateTime publishedAt = candidate.publishedAt();
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
        return Article.create(
                source.getCompany(),
                decision.getTranslatedTitle(),
                candidate.articleUrl(),
                candidate.articleUrlHash(),
                decision.getCategory(),
                publishedAt
        );
    }

    private void storeDiscoveryLogs(
            ArticleCollectionRun run,
            BlogSource source,
            List<ArticleCandidate> candidates
    ) {
        List<ArticleDiscoveryLog> items = candidates.stream()
                .map(candidate -> ArticleDiscoveryLog.create(run, source, candidate))
                .toList();
        collectionItemRepository.saveAll(items);
    }

    private List<ArticleCandidate> observedCandidates(List<ArticleCandidate> candidates) {
        List<ArticleCandidate> observed = new ArrayList<>();
        for (ArticleCandidate candidate : candidates) {
            if (candidate.duplicate()) {
                observed.add(candidate.withDecisionStatus(ArticleCandidateDecisionStatus.DUPLICATE));
            } else {
                observed.add(candidate);
            }
        }
        return observed;
    }

    private List<ArticleCandidate> uniqueCandidates(List<ArticleCandidate> candidates) {
        Map<String, ArticleCandidate> uniqueByHash = new LinkedHashMap<>();
        for (ArticleCandidate candidate : candidates) {
            uniqueByHash.putIfAbsent(candidate.articleUrlHash(), candidate);
        }
        return List.copyOf(uniqueByHash.values());
    }
}
