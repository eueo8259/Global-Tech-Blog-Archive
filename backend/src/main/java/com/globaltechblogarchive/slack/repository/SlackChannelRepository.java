package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackWorkspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SlackChannelRepository extends JpaRepository<SlackChannel, Long> {

    Optional<SlackChannel> findByWorkspaceAndSlackChannelId(
            SlackWorkspace workspace,
            String slackChannelId
    );

    @Query("""
            select distinct channel
            from SlackChannel channel
            join fetch channel.workspace
            join SlackChannelSubscription subscription on subscription.slackChannel = channel
            order by channel.id
            """)
    List<SlackChannel> findAllSubscribedChannelsWithWorkspace();
}
