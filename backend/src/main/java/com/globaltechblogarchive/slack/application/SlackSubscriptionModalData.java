package com.globaltechblogarchive.slack.application;

import java.util.List;

public record SlackSubscriptionModalData(
        String encryptedBotToken,
        List<CompanyOption> companies
) {

    public record CompanyOption(
            String companyKey,
            String companyName,
            boolean selected
    ) {
    }
}
