package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.CrawlPolicy;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
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

    private final ArticleAiDecisionRepository decisionRepository;
    private final ArticleCandidateCollectorRegistry collectorRegistry;
    private final ArticleCandidateFactory candidateFactory;
    private final CrawlPersistenceService persistenceService;
    private final BlogSourceRepository blogSourceRepository;

    public SourceCrawlResult process(Long runId, Long sourceId, CrawlPolicy policy) {
        BlogSource source = blogSourceRepository.findWithCompanyById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Blog source not found: " + sourceId));
        ArticleCandidateCollector collector = collectorRegistry.find(source.getCollectionMethod());
        List<ParsedArticle> cards = collector.collect(source, policy);
        Map<String, ArticleAiDecision> decisionsByHash = findDecisionsByHash(source, cards);
        List<ArticleCandidate> candidates = candidateFactory.create(source, cards, decisionsByHash);
        return persistenceService.persistDiscoveredCandidates(
                runId,
                sourceId,
                candidates,
                decisionsByHash
        );
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
                        ArticleAiReviewService.PROMPT_VERSION
                )
                .stream()
                .collect(Collectors.toMap(
                        ArticleAiDecision::getArticleUrlHash,
                        Function.identity()
                ));
    }

}
