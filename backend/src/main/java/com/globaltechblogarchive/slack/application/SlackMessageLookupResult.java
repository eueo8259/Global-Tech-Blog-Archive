package com.globaltechblogarchive.slack.application;

public record SlackMessageLookupResult(String messageTs, String nextCursor) {

    public static SlackMessageLookupResult found(String messageTs) {
        return new SlackMessageLookupResult(messageTs, null);
    }

    public static SlackMessageLookupResult notFound(String nextCursor) {
        return new SlackMessageLookupResult(null, nextCursor);
    }

    public boolean found() {
        return messageTs != null;
    }

    public boolean hasNextPage() {
        return nextCursor != null && !nextCursor.isBlank();
    }
}
