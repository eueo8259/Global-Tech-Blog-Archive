package com.globaltechblogarchive.global.error.exception;

import com.globaltechblogarchive.global.error.ErrorCode;

public class InvalidInputException extends BusinessException {

    public InvalidInputException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidInputException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
