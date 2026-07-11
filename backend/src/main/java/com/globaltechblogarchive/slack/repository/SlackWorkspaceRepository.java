package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackWorkspaceRepository extends JpaRepository<SlackWorkspace, Long> {

    Optional<SlackWorkspace> findBySlackTeamId(String slackTeamId);
}
