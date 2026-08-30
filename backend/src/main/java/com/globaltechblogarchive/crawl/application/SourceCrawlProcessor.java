package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.application.dto.PreparedSourceCrawl;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceCrawlProcessor {

    private final ArticleAiDecisionRepository decisionRepository;
    private final ArticleCandidateCollectorRegistry collectorRegistry;
    private final ArticleCandidateFactory candidateFactory;
    private final BlogSourceRepository blogSourceRepository;

    public PreparedSourceCrawl prepare(Long sourceId, CrawlPolicy policy) {
        long phaseStartedAt = System.nanoTime();
        BlogSource source = blogSourceRepository.findWithCompanyById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Blog source not found: " + sourceId));
        long sourceLoadDurationNanos = System.nanoTime() - phaseStartedAt;

        phaseStartedAt = System.nanoTime();
        ArticleCandidateCollector collector = collectorRegistry.find(source.getCollectionMethod());
        List<ParsedArticle> cards = collector.collect(source, policy);
        long collectionDurationNanos = System.nanoTime() - phaseStartedAt;

        phaseStartedAt = System.nanoTime();
        Map<String, ArticleAiDecision> decisionsByHash = findDecisionsByHash(source, cards);
        long decisionLookupDurationNanos = System.nanoTime() - phaseStartedAt;

        phaseStartedAt = System.nanoTime();
        List<ArticleCandidate> candidates = candidateFactory.create(source, cards, decisionsByHash);
        long candidateCreationDurationNanos = System.nanoTime() - phaseStartedAt;

        log.info(
                "crawl_source_phase_measurement sourceKey={} sourceLoadMs={} collectionMs={} "
                        + "decisionLookupMs={} candidateCreationMs={} cardCount={} candidateCount={}",
                source.getSourceKey(),
                millis(sourceLoadDurationNanos),
                millis(collectionDurationNanos),
                millis(decisionLookupDurationNanos),
                millis(candidateCreationDurationNanos),
                cards.size(),
                candidates.size()
        );
        return new PreparedSourceCrawl(
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

    private long millis(long durationNanos) {
        return TimeUnit.NANOSECONDS.toMillis(durationNanos);
    }

}
