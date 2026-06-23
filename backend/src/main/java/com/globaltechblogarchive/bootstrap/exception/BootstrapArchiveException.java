package com.globaltechblogarchive.bootstrap.exception;

public class BootstrapArchiveException extends RuntimeException {

    public BootstrapArchiveException(String message) {
        super(message);
    }

    public BootstrapArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
