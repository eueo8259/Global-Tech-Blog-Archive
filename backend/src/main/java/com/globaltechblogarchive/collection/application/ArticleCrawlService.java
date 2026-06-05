package com.globaltechblogarchive.collection.application;

import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.collection.collector.ArticleCandidateCollector;
import com.globaltechblogarchive.collection.collector.ArticleCandidateCollectorRegistry;
import com.globaltechblogarchive.collection.domain.ArticleCandidate;
import com.globaltechblogarchive.collection.domain.ArticleCollectionItem;
import com.globaltechblogarchive.collection.domain.ArticleCollectionRun;
import com.globaltechblogarchive.collection.parser.ParsedArticleCard;
import com.globaltechblogarchive.collection.repository.ArticleCollectionItemRepository;
import com.globaltechblogarchive.collection.repository.ArticleCollectionRunRepository;
import com.globaltechblogarchive.collection.support.CandidateValidationWarnings;
import com.globaltechblogarchive.collection.support.TextCleaner;
import com.globaltechblogarchive.collection.support.UrlHash;
import com.globaltechblogarchive.collection.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import com.globaltechblogarchive.source.repository.BlogSourceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleCrawlService {

    private final BlogSourceRepository blogSourceRepository;
    private final ArticleRepository articleRepository;
    private final ArticleCollectionRunRepository collectionRunRepository;
    private final ArticleCollectionItemRepository collectionItemRepository;
    private final ArticleCandidateCollectorRegistry collectorRegistry;

    @Transactional
    public ArticleCrawlResult run() {
        ArticleCollectionRun run = collectionRunRepository.save(ArticleCollectionRun.start(LocalDateTime.now()));
        List<BlogSource> sources = blogSourceRepository.findByEnabledTrue();
        List<SourceCrawlResult> sourceResults = new ArrayList<>();

        int discoveredCount = 0;
        int duplicateCount = 0;
        int storedCount = 0;
        for (BlogSource source : sources) {
            LocalDateTime collectedAt = LocalDateTime.now();
            try {
                ArticleCandidateCollector collector = collectorRegistry.find(source.getCollectionMethod());
                List<ArticleCandidate> candidates = toCandidates(source, collector.collect(source));
                storedCount += storeCandidates(run, source, candidates);
                discoveredCount += candidates.size();
                duplicateCount += countDuplicates(candidates);
                source.markCollected(collectedAt);
                sourceResults.add(SourceCrawlResult.success(source, candidates));
            } catch (RuntimeException exception) {
                source.markCollectionFailed(collectedAt, exception.getMessage());
                sourceResults.add(SourceCrawlResult.failure(source, exception.getMessage()));
            }
        }

        int successCount = (int) sourceResults.stream().filter(SourceCrawlResult::success).count();
        int failureCount = sourceResults.size() - successCount;
        run.complete(
                LocalDateTime.now(),
                sources.size(),
                successCount,
                failureCount,
                discoveredCount,
                duplicateCount,
                storedCount
        );
        return new ArticleCrawlResult(
                run.getId(),
                sources.size(),
                successCount,
                failureCount,
                discoveredCount,
                duplicateCount,
                storedCount,
                discoveredCount,
                sourceResults
        );
    }

    private List<ArticleCandidate> toCandidates(BlogSource source, List<ParsedArticleCard> cards) {
        List<ArticleCandidate> candidates = new ArrayList<>();
        for (ParsedArticleCard card : cards) {
            String normalizedUrl = UrlNormalizer.normalize(card.originalUrl());
            String normalizedUrlHash = UrlHash.sha256(normalizedUrl);
            boolean duplicate = articleRepository.existsBySourceIdAndNormalizedUrlHash(source.getId(), normalizedUrlHash);
            List<String> validationWarnings = CandidateValidationWarnings.from(source, card, normalizedUrl);
            candidates.add(new ArticleCandidate(
                    source.getCompanyKey(),
                    source.getCompanyName(),
                    card.originalTitle(),
                    card.originalUrl(),
                    card.publishedAt(),
                    TextCleaner.shortContext(card.shortContext(), card.originalTitle()),
                    normalizedUrl,
                    normalizedUrlHash,
                    duplicate,
                    validationWarnings
            ));
        }
        return candidates;
    }

    private int countDuplicates(List<ArticleCandidate> candidates) {
        return (int) candidates.stream().filter(ArticleCandidate::duplicate).count();
    }

    private int storeCandidates(ArticleCollectionRun run, BlogSource source, List<ArticleCandidate> candidates) {
        List<ArticleCollectionItem> items = candidates.stream()
                .map(candidate -> ArticleCollectionItem.create(run, source, candidate))
                .toList();
        collectionItemRepository.saveAll(items);
        return items.size();
    }
}
