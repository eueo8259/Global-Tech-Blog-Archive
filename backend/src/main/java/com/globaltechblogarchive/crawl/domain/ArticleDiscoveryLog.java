package com.globaltechblogarchive.crawl.domain;

import com.globaltechblogarchive.source.domain.BlogSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "article_collections")
public class ArticleDiscoveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ArticleCollectionRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private BlogSource source;

    @Column(name = "company_key", nullable = false, length = 50)
    private String companyKey;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "original_title", nullable = false, length = 500)
    private String originalTitle;

    @Column(name = "article_url", nullable = false, length = 2000)
    private String articleUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Lob
    @Column(name = "short_context", columnDefinition = "TEXT")
    private String shortContext;

    @Column(name = "article_url_hash", nullable = false, length = 64)
    private String articleUrlHash;

    @Column(name = "duplicate_article", nullable = false)
    private boolean duplicate;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_status", nullable = false, length = 30)
    private ArticleCandidateDecisionStatus decisionStatus;

    @Column(name = "validation_warnings", length = 500)
    private String validationWarnings;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static ArticleDiscoveryLog create(
            ArticleCollectionRun run,
            BlogSource source,
            ArticleCandidate candidate
    ) {
        ArticleDiscoveryLog item = new ArticleDiscoveryLog();
        item.run = run;
        item.source = source;
        item.companyKey = candidate.companyKey();
        item.companyName = candidate.companyName();
        item.originalTitle = candidate.originalTitle();
        item.articleUrl = candidate.articleUrl();
        item.publishedAt = candidate.publishedAt();
        item.shortContext = candidate.shortContext();
        item.articleUrlHash = candidate.articleUrlHash();
        item.duplicate = candidate.duplicate();
        item.decisionStatus = candidate.decisionStatus();
        item.validationWarnings = serializeWarnings(candidate.validationWarnings());
        return item;
    }

    public ArticleCandidate toRetryCandidate() {
        return new ArticleCandidate(
                companyKey,
                companyName,
                originalTitle,
                articleUrl,
                publishedAt,
                shortContext,
                articleUrlHash,
                false,
                ArticleCandidateDecisionStatus.NEW,
                deserializeWarnings(validationWarnings)
        );
    }

    private static String serializeWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "";
        }
        return String.join(",", warnings);
    }

    private static List<String> deserializeWarnings(String warnings) {
        if (warnings == null || warnings.isBlank()) {
            return List.of();
        }
        return List.of(warnings.split(","));
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
