package com.globaltechblogarchive.article;

import com.globaltechblogarchive.source.BlogSource;
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

@Entity
@Table(
        name = "articles",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_source_normalized_url_hash",
                columnNames = {"source_id", "normalized_url_hash"}
        )
)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private BlogSource source;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

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

    protected Article() {
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

    public Long getId() {
        return id;
    }

    public BlogSource getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public String getNormalizedUrlHash() {
        return normalizedUrlHash;
    }

    public ArticleCategory getCategory() {
        return category;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
