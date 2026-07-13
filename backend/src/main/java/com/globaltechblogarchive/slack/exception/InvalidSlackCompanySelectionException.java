package com.globaltechblogarchive.slack.exception;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;

public class InvalidSlackCompanySelectionException extends InvalidInputException {

    public InvalidSlackCompanySelectionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidSlackCompanySelectionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
