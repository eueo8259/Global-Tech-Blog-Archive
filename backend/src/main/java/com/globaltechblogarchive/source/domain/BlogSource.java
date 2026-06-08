package com.globaltechblogarchive.source.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "blog_sources")
public class BlogSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "source_key", nullable = false, unique = true, length = 80)
    private String sourceKey;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "site_url", nullable = false, length = 500)
    private String siteUrl;

    @Column(name = "feed_url", length = 500)
    private String feedUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_method", nullable = false, length = 30)
    private CollectionMethod collectionMethod;

    @Column(name = "enabled", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean enabled = true;

    @Column(name = "last_collected_at")
    private LocalDateTime lastCollectedAt;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    @Column(name = "last_error_msg", length = 500)
    private String lastErrorMsg;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static BlogSource create(
            Company company,
            String sourceKey,
            String sourceName,
            String siteUrl,
            String feedUrl,
            CollectionMethod collectionMethod
    ) {
        BlogSource source = new BlogSource();
        source.company = company;
        source.sourceKey = sourceKey;
        source.sourceName = sourceName;
        source.siteUrl = siteUrl;
        source.feedUrl = feedUrl;
        source.collectionMethod = collectionMethod;
        return source;
    }

    public void markCollected(LocalDateTime collectedAt) {
        lastCollectedAt = collectedAt;
        lastErrorAt = null;
        lastErrorMsg = null;
    }

    public void markCollectionFailed(LocalDateTime failedAt, String errorMessage) {
        lastErrorAt = failedAt;
        lastErrorMsg = truncateErrorMessage(errorMessage);
    }

    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= 500) {
            return errorMessage;
        }
        return errorMessage.substring(0, 500);
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
