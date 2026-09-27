package com.globaltechblogarchive.slack.application.digest;

public record SlackDailyDigestRunResult(
        boolean started,
        int recoveredCount,
        int createdCount,
        int readyCount
) {

    public static SlackDailyDigestRunResult skipped() {
        return new SlackDailyDigestRunResult(false, 0, 0, 0);
    }
}
