package com.globaltechblogarchive.article.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.BusinessException;

public class ArticleMetadataAiClientException extends BusinessException {

    public ArticleMetadataAiClientException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ArticleMetadataAiClientException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
