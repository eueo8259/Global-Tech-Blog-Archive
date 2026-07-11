package com.globaltechblogarchive.slack.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.BusinessException;

public class SlackOAuthException extends BusinessException {

    public SlackOAuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SlackOAuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
