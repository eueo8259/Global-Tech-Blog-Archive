package com.globaltechblogarchive.slack.repository;

import java.time.LocalDateTime;

public interface SlackDigestArticleProjection {

    Long getArticleId();

    Long getCompanyId();

    String getCompanyName();

    String getTitle();

    String getArticleUrl();

    LocalDateTime getPublishedAt();
}
