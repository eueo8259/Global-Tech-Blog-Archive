package com.globaltechblogarchive.article.exception;

public class ArticleMetadataAiClientException extends RuntimeException {

    public ArticleMetadataAiClientException(String message) {
        super(message);
    }

    public ArticleMetadataAiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
