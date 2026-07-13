package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlackWorkspaceRepository extends JpaRepository<SlackWorkspace, Long> {

    Optional<SlackWorkspace> findBySlackTeamId(String slackTeamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workspace from SlackWorkspace workspace where workspace.slackTeamId = :slackTeamId")
    Optional<SlackWorkspace> findBySlackTeamIdForUpdate(@Param("slackTeamId") String slackTeamId);
}
