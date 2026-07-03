package com.globaltechblogarchive.crawl.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.BusinessException;

public class SourceCollectionException extends BusinessException {

    public SourceCollectionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SourceCollectionException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
