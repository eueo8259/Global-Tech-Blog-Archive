package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackChannelRepository extends JpaRepository<SlackChannel, Long> {

    Optional<SlackChannel> findByWorkspaceAndSlackChannelId(
            SlackWorkspace workspace,
            String slackChannelId
    );
}
