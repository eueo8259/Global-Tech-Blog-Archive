package com.globaltechblogarchive.crawl.domain;

import com.globaltechblogarchive.company.domain.Company;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "article_candidates",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_article_candidates_company_url_hash",
                columnNames = {"company_id", "article_url_hash"}
        )
)
public class ArticleCandidateTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private BlogSource source;

    @Column(name = "article_url", nullable = false, length = 2000)
    private String articleUrl;

    @Column(name = "article_url_hash", nullable = false, length = 64)
    private String articleUrlHash;

    @Column(name = "original_title", nullable = false, length = 500)
    private String originalTitle;

    @Lob
    @Column(name = "short_context", columnDefinition = "TEXT")
    private String shortContext;

    @Column(name = "category_hint", length = 100)
    private String categoryHint;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ArticleCandidateDecisionStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "processing_prompt_version", length = 50)
    private String processingPromptVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ArticleCandidateTask create(
            Company company,
            BlogSource source,
            ArticleCandidate candidate,
            ArticleCandidateDecisionStatus status,
            String promptVersion
    ) {
        ArticleCandidateTask task = new ArticleCandidateTask();
        task.company = company;
        task.source = source;
        task.refresh(source, candidate);
        task.status = status;
        if (status != ArticleCandidateDecisionStatus.NEW
                && status != ArticleCandidateDecisionStatus.DUPLICATE) {
            task.processingPromptVersion = promptVersion;
        }
        return task;
    }

    public void refresh(BlogSource source, ArticleCandidate candidate) {
        this.source = source;
        this.articleUrl = candidate.articleUrl();
        this.articleUrlHash = candidate.articleUrlHash();
        this.originalTitle = candidate.originalTitle();
        this.shortContext = candidate.shortContext();
        this.categoryHint = candidate.categoryHint();
        if (candidate.publishedAt() != null) {
            this.publishedAt = candidate.publishedAt();
        }
    }

    public void observe(
            BlogSource source,
            ArticleCandidate candidate,
            ArticleCandidateDecisionStatus observedStatus,
            String promptVersion
    ) {
        refresh(source, candidate);
        if (observedStatus != ArticleCandidateDecisionStatus.NEW) {
            status = observedStatus;
            processingPromptVersion = promptVersion;
            if (observedStatus == ArticleCandidateDecisionStatus.DUPLICATE) {
                processingPromptVersion = null;
            }
            nextRetryAt = null;
            processingStartedAt = null;
            lastErrorCode = null;
            lastErrorMessage = null;
            return;
        }
        if (status == ArticleCandidateDecisionStatus.DUPLICATE
                || (processingPromptVersion != null
                && !processingPromptVersion.equals(promptVersion))) {
            resetForReview();
        }
    }

    public void claim(LocalDateTime processingStartedAt, String promptVersion) {
        if (status != ArticleCandidateDecisionStatus.NEW
                && status != ArticleCandidateDecisionStatus.AI_RETRY_WAITING
                && status != ArticleCandidateDecisionStatus.AI_FAILED) {
            throw new IllegalStateException("Candidate cannot be claimed from status: " + status);
        }
        status = ArticleCandidateDecisionStatus.AI_PROCESSING;
        attemptCount++;
        this.processingStartedAt = processingStartedAt;
        this.processingPromptVersion = promptVersion;
        nextRetryAt = null;
        lastErrorCode = null;
        lastErrorMessage = null;
    }

    public void approve() {
        complete(ArticleCandidateDecisionStatus.AI_APPROVED);
    }

    public void reject() {
        complete(ArticleCandidateDecisionStatus.AI_REJECTED);
    }

    public void retryAt(LocalDateTime retryAt, String errorCode, String errorMessage) {
        status = ArticleCandidateDecisionStatus.AI_RETRY_WAITING;
        nextRetryAt = retryAt;
        processingStartedAt = null;
        lastErrorCode = errorCode;
        lastErrorMessage = truncate(errorMessage);
    }

    public void fail(String errorCode, String errorMessage) {
        status = ArticleCandidateDecisionStatus.AI_FAILED;
        nextRetryAt = null;
        processingStartedAt = null;
        lastErrorCode = errorCode;
        lastErrorMessage = truncate(errorMessage);
    }

    public void recover(LocalDateTime retryAt) {
        retryAt(retryAt, "STALE_PROCESSING", "Recovered stale AI processing candidate");
    }

    public boolean isProcessing() {
        return status == ArticleCandidateDecisionStatus.AI_PROCESSING;
    }

    public boolean matchesClaim(LocalDateTime claimedAt, String promptVersion) {
        return isProcessing()
                && Objects.equals(processingStartedAt, claimedAt)
                && Objects.equals(processingPromptVersion, promptVersion);
    }

    private void complete(ArticleCandidateDecisionStatus completedStatus) {
        status = completedStatus;
        nextRetryAt = null;
        processingStartedAt = null;
        lastErrorCode = null;
        lastErrorMessage = null;
    }

    private void resetForReview() {
        status = ArticleCandidateDecisionStatus.NEW;
        attemptCount = 0;
        nextRetryAt = null;
        processingStartedAt = null;
        lastErrorCode = null;
        lastErrorMessage = null;
        processingPromptVersion = null;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
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
