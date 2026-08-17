package com.globaltechblogarchive.crawl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CrawlExecutionConfig {

    @Bean(name = "crawlSourceExecutor")
    public ThreadPoolTaskExecutor crawlSourceExecutor(CrawlExecutionProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.sourceConcurrency());
        executor.setMaxPoolSize(properties.sourceConcurrency());
        executor.setThreadNamePrefix("crawl-source-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        return executor;
    }
}
