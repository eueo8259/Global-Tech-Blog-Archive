package com.globaltechblogarchive.crawl.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.BusinessException;

public class SourceFetchException extends BusinessException {

    private final Integer statusCode;

    public SourceFetchException(
            ErrorCode errorCode,
            Integer statusCode,
            String message,
            Throwable cause
    ) {
        super(errorCode, message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
