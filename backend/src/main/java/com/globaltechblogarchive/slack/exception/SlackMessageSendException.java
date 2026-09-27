package com.globaltechblogarchive.slack.exception;

import com.globaltechblogarchive.slack.application.SlackSendCertainty;
import java.time.Duration;

public class SlackMessageSendException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;
    private final Duration retryAfter;
    private final SlackSendCertainty certainty;

    public SlackMessageSendException(
            String errorCode,
            String message,
            boolean retryable,
            Duration retryAfter,
            SlackSendCertainty certainty
    ) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.retryAfter = retryAfter;
        this.certainty = certainty;
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

    public SlackSendCertainty getCertainty() {
        return certainty;
    }
}
