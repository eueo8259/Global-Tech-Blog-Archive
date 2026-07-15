package com.globaltechblogarchive.crawl.application;

import com.globaltechblogarchive.crawl.domain.ArticleAiDecision;
import com.globaltechblogarchive.article.repository.ArticleRepository;
import com.globaltechblogarchive.crawl.domain.ArticleCandidate;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.parser.ParsedArticle;
import com.globaltechblogarchive.crawl.support.CandidateValidationWarnings;
import com.globaltechblogarchive.crawl.support.TextCleaner;
import com.globaltechblogarchive.crawl.support.UrlHash;
import com.globaltechblogarchive.crawl.support.UrlNormalizer;
import com.globaltechblogarchive.source.domain.BlogSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleCandidateFactory {

    private final ArticleRepository articleRepository;

    public List<ArticleCandidate> create(
            BlogSource source,
            List<ParsedArticle> cards,
            Map<String, ArticleAiDecision> decisionsByHash
    ) {
        if (cards.isEmpty()) {
            return List.of();
        }
        List<String> hashes = cards.stream()
                .map(ParsedArticle::originalUrl)
                .map(UrlNormalizer::normalize)
                .map(UrlHash::sha256)
                .distinct()
                .toList();
        Set<String> existingHashes = articleRepository.findExistingHashes(
                source.getCompany().getId(),
                hashes
        );

        List<ArticleCandidate> candidates = new ArrayList<>();
        for (ParsedArticle card : cards) {
            String articleUrl = UrlNormalizer.normalize(card.originalUrl());
            String articleUrlHash = UrlHash.sha256(articleUrl);
            ArticleAiDecision decision = decisionsByHash.get(articleUrlHash);
            boolean duplicate = existingHashes.contains(articleUrlHash);
            candidates.add(new ArticleCandidate(
                    source.getCompany().getCompanyKey(),
                    source.getCompany().getCompanyName(),
                    card.originalTitle(),
                    articleUrl,
                    card.publishedAt(),
                    TextCleaner.shortContext(card.shortContext(), card.originalTitle()),
                    card.categoryHint(),
                    articleUrlHash,
                    duplicate,
                    decisionStatus(decision),
                    CandidateValidationWarnings.from(source, card, articleUrl)
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
