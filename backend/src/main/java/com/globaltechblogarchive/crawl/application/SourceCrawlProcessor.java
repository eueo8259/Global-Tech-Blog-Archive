package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.domain.CrawlMode;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.crawl.support.UrlHash;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SourceCrawlProcessor {

    private static final String PROMPT_VERSION = "v1";

    private final ArticleAiDecisionRepository decisionRepository;
    private final ArticleDiscoveryLogRepository collectionItemRepository;
    private final ArticleCandidateCollectorRegistry collectorRegistry;
    private final ArticleCandidateFactory candidateFactory;
    private final ArticleDecisionProcessor decisionProcessor;
    private final CrawlPersistenceService persistenceService;
    private final BlogSourceRepository blogSourceRepository;

    public SourceCrawlResult process(Long runId, Long sourceId, CrawlMode mode) {
        BlogSource source = blogSourceRepository.findWithCompanyById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Blog source not found: " + sourceId));
        ArticleCandidateCollector collector = collectorRegistry.find(source.getCollectionMethod());
        List<ParsedArticle> cards = collector.collect(source, mode);
        Map<String, ArticleAiDecision> decisionsByHash = findDecisionsByHash(source, cards);
        List<ArticleCandidate> candidates = candidateFactory.create(source, cards, decisionsByHash);
        Map<String, PreparedArticleDecision> preparedDecisions = decisionsByHash.values().stream()
                .map(PreparedArticleDecision::from)
                .collect(Collectors.toMap(
                        PreparedArticleDecision::articleUrlHash,
                        Function.identity()
                ));
        ProcessedCandidates processed = decisionProcessor.process(
                source,
                candidates,
                preparedDecisions,
                PROMPT_VERSION
        );
        return persistenceService.persistCollection(runId, sourceId, processed);
    }

    public SourceCrawlResult retryAiFailures(Long runId, Long sourceId, List<Long> failureLogIds) {
        BlogSource source = blogSourceRepository.findWithCompanyById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Blog source not found: " + sourceId));
        List<ArticleCandidate> candidates = collectionItemRepository.findAllById(failureLogIds).stream()
                .map(ArticleDiscoveryLog::toRetryCandidate)
                .toList();
        ProcessedCandidates processed = decisionProcessor.process(
                source,
                candidates,
                new java.util.HashMap<>(),
                PROMPT_VERSION
        );
        return persistenceService.persistRetry(runId, sourceId, processed);
    }

    private Map<String, ArticleAiDecision> findDecisionsByHash(
            BlogSource source,
            List<ParsedArticle> cards
    ) {
        if (cards.isEmpty()) {
            return Map.of();
        }
        List<String> hashes = cards.stream()
                .map(ParsedArticle::originalUrl)
                .map(UrlNormalizer::normalize)
                .map(UrlHash::sha256)
                .distinct()
                .toList();

        return decisionRepository.findByCompanyIdAndArticleUrlHashInAndPromptVersion(
                        source.getCompany().getId(),
                        hashes,
                        PROMPT_VERSION
                )
                .stream()
                .collect(Collectors.toMap(
                        ArticleAiDecision::getArticleUrlHash,
                        Function.identity()
                ));
    }

}
