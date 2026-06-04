package com.globaltechblogarchive.global.error.exception;

import com.globaltechblogarchive.global.error.ErrorCode;

public class InvalidInputException extends BusinessException {

    public InvalidInputException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}
