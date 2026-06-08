package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.domain.ArticleAiDecision;
import com.globaltechblogarchive.article.repository.ArticleAiDecisionRepository;
import com.globaltechblogarchive.crawl.application.ArticleDecisionProcessor.ProcessedCandidates;
import com.globaltechblogarchive.crawl.application.dto.SourceCrawlResult;
import com.globaltechblogarchive.crawl.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionItem;
import com.globaltechblogarchive.crawl.domain.ArticleCollectionRun;
import com.globaltechblogarchive.crawl.parser.ParsedArticleCard;
import com.globaltechblogarchive.crawl.repository.ArticleCollectionItemRepository;
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

    private final ArticleAiDecisionRepository decisionRepository;
    private final ArticleCollectionItemRepository collectionItemRepository;
    private final ArticleCandidateCollectorRegistry collectorRegistry;
    private final ArticleCandidateFactory candidateFactory;
    private final ArticleDecisionProcessor decisionProcessor;

    public SourceCrawlOutcome process(ArticleCollectionRun run, BlogSource source, String promptVersion) {
        LocalDateTime collectedAt = LocalDateTime.now();
        try {
            ArticleCandidateCollector collector = collectorRegistry.find(source.getCollectionMethod());
            List<ParsedArticleCard> cards = collector.collect(source);
            Map<String, ArticleAiDecision> decisionsByHash = findDecisionsByHash(source, cards, promptVersion);
            List<ArticleCandidate> candidates = candidateFactory.create(source, cards, decisionsByHash);
            ProcessedCandidates processed = decisionProcessor.process(
                    source,
                    candidates,
                    decisionsByHash,
                    promptVersion
            );
            storeCandidates(run, source, processed.candidates());
            source.markCollected(collectedAt);
            return new SourceCrawlOutcome(
                    SourceCrawlResult.success(source, processed.candidates()),
                    CrawlRunSummary.from(processed)
            );
        } catch (RuntimeException exception) {
            source.markCollectionFailed(collectedAt, exception.getMessage());
            return new SourceCrawlOutcome(
                    SourceCrawlResult.failure(source, exception.getMessage()),
                    CrawlRunSummary.empty()
            );
        }
    }

    private Map<String, ArticleAiDecision> findDecisionsByHash(
            BlogSource source,
            List<ParsedArticleCard> cards,
            String promptVersion
    ) {
        if (cards.isEmpty()) {
            return Map.of();
        }
        List<String> hashes = cards.stream()
                .map(ParsedArticleCard::originalUrl)
                .map(UrlNormalizer::normalize)
                .map(UrlHash::sha256)
                .distinct()
                .toList();
        return decisionRepository.findBySourceIdAndNormalizedUrlHashInAndPromptVersion(
                        source.getId(),
                        hashes,
                        promptVersion
                )
                .stream()
                .collect(Collectors.toMap(
                        ArticleAiDecision::getNormalizedUrlHash,
                        Function.identity()
                ));
    }

    private void storeCandidates(ArticleCollectionRun run, BlogSource source, List<ArticleCandidate> candidates) {
        List<ArticleCollectionItem> items = candidates.stream()
                .map(candidate -> ArticleCollectionItem.create(run, source, candidate))
                .toList();
        collectionItemRepository.saveAll(items);
    }
}
