package com.globaltechblogarchive.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "blog_sources")
public class BlogSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_key", nullable = false, unique = true, length = 50)
    private String companyKey;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

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

    protected BlogSource() {
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

    public String getCompanyKey() {
        return companyKey;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public CollectionMethod getCollectionMethod() {
        return collectionMethod;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getLastCollectedAt() {
        return lastCollectedAt;
    }

    public LocalDateTime getLastErrorAt() {
        return lastErrorAt;
    }

    public String getLastErrorMsg() {
        return lastErrorMsg;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
