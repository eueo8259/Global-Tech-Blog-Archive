package com.globaltechblogarchive.crawl.repository;

import com.globaltechblogarchive.crawl.domain.ArticleCandidateDecisionStatus;
import com.globaltechblogarchive.crawl.domain.ArticleCandidateTask;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleCandidateTaskRepository extends JpaRepository<ArticleCandidateTask, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"company", "source"})
    List<ArticleCandidateTask> findByCompanyIdAndArticleUrlHashIn(
            Long companyId,
            Collection<String> articleUrlHashes
    );

    Optional<ArticleCandidateTask> findByCompanyIdAndArticleUrlHash(Long companyId, String articleUrlHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT candidate FROM ArticleCandidateTask candidate WHERE candidate.id = :id")
    Optional<ArticleCandidateTask> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"company", "source"})
    @Query("""
            SELECT candidate
            FROM ArticleCandidateTask candidate
            WHERE candidate.status = :newStatus
               OR (candidate.status = :retryStatus AND candidate.nextRetryAt <= :now)
            ORDER BY candidate.createdAt, candidate.id
            """)
    List<ArticleCandidateTask> findClaimable(
            @Param("newStatus") ArticleCandidateDecisionStatus newStatus,
            @Param("retryStatus") ArticleCandidateDecisionStatus retryStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT candidate
            FROM ArticleCandidateTask candidate
            WHERE candidate.status = :status
              AND candidate.processingStartedAt < :threshold
            ORDER BY candidate.processingStartedAt, candidate.id
            """)
    List<ArticleCandidateTask> findStaleProcessing(
            @Param("status") ArticleCandidateDecisionStatus status,
            @Param("threshold") LocalDateTime threshold
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"company", "source"})
    List<ArticleCandidateTask> findByStatusOrderByUpdatedAtAscIdAsc(
            ArticleCandidateDecisionStatus status,
            Pageable pageable
    );
}
