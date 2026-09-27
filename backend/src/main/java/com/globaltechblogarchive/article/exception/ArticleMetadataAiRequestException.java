package com.globaltechblogarchive.article.exception;

import lombok.Getter;

@Getter
public class ArticleMetadataAiRequestException extends RuntimeException {

    private final String failureCode;
    private final boolean retryable;

    public ArticleMetadataAiRequestException(
            String failureCode,
            String message,
            boolean retryable
    ) {
        super(message);
        this.failureCode = failureCode;
        this.retryable = retryable;
    }
}
