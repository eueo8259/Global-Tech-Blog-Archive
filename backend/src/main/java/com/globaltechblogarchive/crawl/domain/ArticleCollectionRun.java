package com.globaltechblogarchive.crawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "article_collection_runs")
public class ArticleCollectionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "source_count", nullable = false)
    private int sourceCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "discovered_count", nullable = false)
    private int discoveredCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Column(name = "stored_count", nullable = false)
    private int storedCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static ArticleCollectionRun start(LocalDateTime startedAt) {
        ArticleCollectionRun run = new ArticleCollectionRun();
        run.startedAt = startedAt;
        return run;
    }

    public void complete(
            LocalDateTime finishedAt,
            int sourceCount,
            int successCount,
            int failureCount,
            int discoveredCount,
            int duplicateCount,
            int storedCount
    ) {
        this.finishedAt = finishedAt;
        this.sourceCount = sourceCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.discoveredCount = discoveredCount;
        this.duplicateCount = duplicateCount;
        this.storedCount = storedCount;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

