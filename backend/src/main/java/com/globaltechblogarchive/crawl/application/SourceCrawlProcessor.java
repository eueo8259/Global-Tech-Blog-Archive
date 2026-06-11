package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.CrawlRunSummary;
import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.crawl.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleDiscoveryLog;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.repository.ArticleDiscoveryLogRepository;
import com.globaltechblogarchive.crawl.support.UrlHash;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.time.LocalDateTime;
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

    public SourceCrawlResult process(ArticleCollectionRun run, BlogSource source) {
        LocalDateTime collectedAt = LocalDateTime.now();
        try {
            ArticleCandidateCollector collector = collectorRegistry.find(source.getCollectionMethod());
            List<ParsedArticle> cards = collector.collect(source);
            Map<String, ArticleAiDecision> decisionsByHash = findDecisionsByHash(source, cards);
            List<ArticleCandidate> candidates = candidateFactory.create(source, cards, decisionsByHash);
            ProcessedCandidates processed = decisionProcessor.process(
                    source,
                    candidates,
                    decisionsByHash,
                    PROMPT_VERSION
            );
            storeCandidates(run, source, processed.candidates());
            source.markCollected(collectedAt);

            CrawlRunSummary summary = CrawlRunSummary.from(processed);
            return SourceCrawlResult.success(source, processed.candidates(), summary);
        } catch (RuntimeException exception) {
            source.markCollectionFailed(collectedAt, exception.getMessage());
            return SourceCrawlResult.failure(source, exception.getMessage());
        }
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

    private void storeCandidates(ArticleCollectionRun run, BlogSource source, List<ArticleCandidate> candidates) {
        List<ArticleDiscoveryLog> items = candidates.stream()
                .map(candidate -> ArticleDiscoveryLog.create(run, source, candidate))
                .toList();
        collectionItemRepository.saveAll(items);
    }
}
