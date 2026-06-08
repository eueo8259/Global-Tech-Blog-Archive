package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.article.domain.ArticleAiDecision;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.parser.ParsedArticleCard;
import com.globaltechblogarchive.crawl.support.CandidateValidationWarnings;
import com.globaltechblogarchive.crawl.support.TextCleaner;
import com.globaltechblogarchive.crawl.support.UrlHash;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleCandidateFactory {

    private final ArticleRepository articleRepository;

    public List<ArticleCandidate> create(
            BlogSource source,
            List<ParsedArticleCard> cards,
            Map<String, ArticleAiDecision> decisionsByHash
    ) {
        List<ArticleCandidate> candidates = new ArrayList<>();
        for (ParsedArticleCard card : cards) {
            String normalizedUrl = UrlNormalizer.normalize(card.originalUrl());
            String normalizedUrlHash = UrlHash.sha256(normalizedUrl);
            ArticleAiDecision decision = decisionsByHash.get(normalizedUrlHash);
            boolean duplicate = articleRepository.existsBySourceIdAndNormalizedUrlHash(
                    source.getId(),
                    normalizedUrlHash
            );
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
                    decisionStatus(decision),
                    CandidateValidationWarnings.from(source, card, normalizedUrl)
            ));
        }
        return candidates;
    }

    private ArticleCandidateDecisionStatus decisionStatus(ArticleAiDecision decision) {
        if (decision == null) {
            return ArticleCandidateDecisionStatus.NEW;
        }
        if (decision.isSaveTarget()) {
            return ArticleCandidateDecisionStatus.PREVIOUSLY_APPROVED;
        }
        return ArticleCandidateDecisionStatus.PREVIOUSLY_REJECTED;
    }
}
