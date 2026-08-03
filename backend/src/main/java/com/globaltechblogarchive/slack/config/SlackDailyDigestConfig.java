package com.globaltechblogarchive.slack.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackDailyDigestConfig {

    @Bean
    public Clock slackDailyDigestClock(SlackDailyDigestProperties properties) {
        return Clock.system(ZoneId.of(properties.zone()));
    }
}
