package com.globaltechblogarchive.slack.exception;

import java.time.Duration;

public class SlackMessageSendException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;
    private final Duration retryAfter;

    public SlackMessageSendException(
            String errorCode,
            String message,
            boolean retryable,
            Duration retryAfter
    ) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.retryAfter = retryAfter;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
