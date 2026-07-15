package com.globaltechblogarchive.slack.repository;

import com.globaltechblogarchive.slack.domain.SlackChannel;
import com.globaltechblogarchive.slack.domain.SlackChannelSubscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlackChannelSubscriptionRepository extends JpaRepository<SlackChannelSubscription, Long> {

    List<SlackChannelSubscription> findAllBySlackChannel(SlackChannel slackChannel);

    @Query("""
            select company.companyKey
            from SlackChannelSubscription subscription
            join subscription.company company
            join subscription.slackChannel channel
            join channel.workspace workspace
            where workspace.slackTeamId = :teamId
              and channel.slackChannelId = :channelId
            """)
    List<String> findCompanyKeysByTeamIdAndChannelId(
            @Param("teamId") String teamId,
            @Param("channelId") String channelId
    );
}
