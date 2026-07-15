package com.globaltechblogarchive.slack.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.BusinessException;

public class SlackViewException extends BusinessException {

    public SlackViewException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
