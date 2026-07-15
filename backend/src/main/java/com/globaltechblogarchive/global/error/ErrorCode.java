package com.globaltechblogarchive.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal server error"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C003", "Unsupported media type"),
    ARTICLE_METADATA_AI_CLIENT_ERROR(HttpStatus.BAD_GATEWAY, "A001", "Article metadata AI client error"),
    SOURCE_FETCH_DNS_ERROR(HttpStatus.BAD_GATEWAY, "S001", "Source DNS lookup failed"),
    SOURCE_FETCH_CONNECTION_ERROR(HttpStatus.BAD_GATEWAY, "S002", "Source connection failed"),
    SOURCE_FETCH_TLS_ERROR(HttpStatus.BAD_GATEWAY, "S003", "Source TLS handshake failed"),
    SOURCE_FETCH_TIMEOUT_ERROR(HttpStatus.GATEWAY_TIMEOUT, "S004", "Source request timed out"),
    SOURCE_FETCH_HTTP_STATUS_ERROR(HttpStatus.BAD_GATEWAY, "S005", "Source returned an error response"),
    SOURCE_FETCH_NETWORK_ERROR(HttpStatus.BAD_GATEWAY, "S006", "Source network request failed"),
    SOURCE_FETCH_INTERRUPTED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S007", "Source request interrupted"),
    SOURCE_COLLECTION_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S008", "Source collection configuration error"),
    SOURCE_CONTENT_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "S009", "Source content parsing failed"),
    SLACK_TOKEN_DECRYPTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SL001", "Slack token decryption failed"),
    SLACK_OAUTH_ERROR(HttpStatus.BAD_GATEWAY, "SL002", "Slack OAuth request failed"),
    SLACK_VIEW_OPEN_ERROR(HttpStatus.BAD_GATEWAY, "SL003", "Slack view open request failed");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
