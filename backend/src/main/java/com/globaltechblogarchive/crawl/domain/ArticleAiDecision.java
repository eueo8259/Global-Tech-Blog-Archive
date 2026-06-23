package com.globaltechblogarchive.crawl.domain;

import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.company.domain.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "article_ai_decisions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_article_ai_decision_company_url_hash_prompt",
                columnNames = {"company_id", "article_url_hash", "prompt_version"}
        )
)
public class ArticleAiDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "article_url_hash", nullable = false, length = 64)
    private String articleUrlHash;

    @Column(name = "article_url", nullable = false, length = 2000)
    private String articleUrl;

    @Column(name = "original_title", nullable = false, length = 500)
    private String originalTitle;

    @Column(name = "translated_title", nullable = false, length = 500)
    private String translatedTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ArticleCategory category;

    @Column(name = "save_target", nullable = false)
    private boolean saveTarget;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ArticleAiDecision create(
            Company company,
            String articleUrlHash,
            String articleUrl,
            String originalTitle,
            String translatedTitle,
            ArticleCategory category,
            boolean saveTarget,
            String model,
            String promptVersion
    ) {
        ArticleAiDecision decision = new ArticleAiDecision();
        decision.company = company;
        decision.articleUrlHash = articleUrlHash;
        decision.articleUrl = articleUrl;
        decision.originalTitle = originalTitle;
        decision.translatedTitle = translatedTitle;
        decision.category = category;
        decision.saveTarget = saveTarget;
        decision.model = model;
        decision.promptVersion = promptVersion;
        return decision;
    }

    public static ArticleAiDecision restore(
            Company company,
            String articleUrlHash,
            String articleUrl,
            String originalTitle,
            String translatedTitle,
            ArticleCategory category,
            boolean saveTarget,
            String model,
            String promptVersion,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        ArticleAiDecision decision = create(
                company,
                articleUrlHash,
                articleUrl,
                originalTitle,
                translatedTitle,
                category,
                saveTarget,
                model,
                promptVersion
        );
        decision.createdAt = createdAt;
        decision.updatedAt = updatedAt;
        return decision;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
