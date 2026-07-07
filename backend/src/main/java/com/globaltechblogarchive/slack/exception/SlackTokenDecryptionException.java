package com.globaltechblogarchive.slack.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.BusinessException;

public class SlackTokenDecryptionException extends BusinessException {

    public SlackTokenDecryptionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SlackTokenDecryptionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
