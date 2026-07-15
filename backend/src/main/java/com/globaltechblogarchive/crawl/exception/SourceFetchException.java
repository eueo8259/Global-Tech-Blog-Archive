package com.globaltechblogarchive.crawl.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.BusinessException;

public class SourceFetchException extends BusinessException {

    public SourceFetchException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SourceFetchException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
