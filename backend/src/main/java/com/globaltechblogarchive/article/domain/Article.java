package com.globaltechblogarchive.article.domain;

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
        name = "articles",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_source_normalized_url_hash",
                columnNames = {"company_id", "normalized_url_hash"}
        )
)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "original_url", nullable = false, length = 2000)
    private String originalUrl;

    @Column(name = "normalized_url", nullable = false, length = 2000)
    private String normalizedUrl;

    @Column(name = "normalized_url_hash", nullable = false, length = 64)
    private String normalizedUrlHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ArticleCategory category;

    // Collectors must use collection time when the source does not provide a publication time.
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Article create(
            Company company,
            String title,
            String originalUrl,
            String normalizedUrl,
            String normalizedUrlHash,
            ArticleCategory category,
            LocalDateTime publishedAt
    ) {
        Article article = new Article();
        article.company = company;
        article.title = title;
        article.originalUrl = originalUrl;
        article.normalizedUrl = normalizedUrl;
        article.normalizedUrlHash = normalizedUrlHash;
        article.category = category;
        article.publishedAt = publishedAt;
        return article;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
