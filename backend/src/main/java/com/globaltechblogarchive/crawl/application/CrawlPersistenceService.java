package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.domain.Article;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates;
import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlPersistenceService {

    private final ArticleRepository articleRepository;
    private final ArticleAiDecisionRepository decisionRepository;
    private final ArticleDiscoveryLogRepository collectionItemRepository;
    private final ArticleCollectionRunRepository collectionRunRepository;
    private final BlogSourceRepository blogSourceRepository;

    @Transactional
    public SourceCrawlResult persistCollection(
            Long runId,
            Long sourceId,
            ProcessedCandidates processed
    ) {
        return persist(runId, sourceId, processed, true);
    }

    @Transactional
    public SourceCrawlResult persistRetry(
            Long runId,
            Long sourceId,
            ProcessedCandidates processed
    ) {
        return persist(runId, sourceId, processed, false);
    }

    private SourceCrawlResult persist(
            Long runId,
            Long sourceId,
            ProcessedCandidates processed,
            boolean markCollected
    ) {
        ArticleCollectionRun run = collectionRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Collection run not found: " + runId));
        BlogSource source = blogSourceRepository.findWithCompanyById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Blog source not found: " + sourceId));

        saveNewDecisions(source, processed.newDecisions());
        int storedArticleCount = saveApprovedArticles(source, processed);
        storeCandidates(run, source, processed.candidates());
        if (markCollected) {
            source.markCollected(LocalDateTime.now());
        }

        CrawlRunSummary summary = CrawlRunSummary.from(
                processed.candidates(),
                storedArticleCount
        );
        return SourceCrawlResult.success(source, processed.candidates(), summary);
    }

    private void saveNewDecisions(
            BlogSource source,
            List<PreparedArticleDecision> decisions
    ) {
        List<ArticleAiDecision> entities = decisions.stream()
                .map(decision -> ArticleAiDecision.create(
                        source.getCompany(),
                        decision.articleUrlHash(),
                        decision.articleUrl(),
                        decision.originalTitle(),
                        decision.translatedTitle(),
                        decision.category(),
                        decision.saveTarget(),
                        decision.model(),
                        decision.promptVersion()
                ))
                .toList();
        if (!entities.isEmpty()) {
            decisionRepository.saveAll(entities);
        }
    }

    private int saveApprovedArticles(
            BlogSource source,
            ProcessedCandidates processed
    ) {
        List<Article> articles = processed.candidates().stream()
                .filter(candidate -> !candidate.duplicate())
                .filter(candidate -> {
                    PreparedArticleDecision decision =
                            processed.decisionsByHash().get(candidate.articleUrlHash());
                    return decision != null && isSaveTarget(decision);
                })
                .map(candidate -> toArticle(source, candidate, processed))
                .toList();
        if (!articles.isEmpty()) {
            articleRepository.saveAll(articles);
        }
        return articles.size();
    }

    private boolean isSaveTarget(PreparedArticleDecision decision) {
        return decision.saveTarget() && decision.category() != ArticleCategory.ELSE;
    }

    private Article toArticle(
            BlogSource source,
            ArticleCandidate candidate,
            ProcessedCandidates processed
    ) {
        PreparedArticleDecision decision =
                processed.decisionsByHash().get(candidate.articleUrlHash());
        LocalDateTime publishedAt = candidate.publishedAt();
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
        return Article.create(
                source.getCompany(),
                decision.translatedTitle(),
                candidate.articleUrl(),
                candidate.articleUrlHash(),
                decision.category(),
                publishedAt
        );
    }

    private void storeCandidates(
            ArticleCollectionRun run,
            BlogSource source,
            List<ArticleCandidate> candidates
    ) {
        List<ArticleDiscoveryLog> items = candidates.stream()
                .map(candidate -> ArticleDiscoveryLog.create(run, source, candidate))
                .toList();
        collectionItemRepository.saveAll(items);
    }
}
