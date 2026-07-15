package com.globaltechblogarchive.slack.api.dto;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import org.springframework.util.StringUtils;

public record SlackTestDeliveryRequest(
        String companyKey,
        String title,
        String articleUrl
) {

    public void validate() {
        requireText(companyKey, "companyKey");
        requireText(title, "title");
        requireText(articleUrl, "articleUrl");
    }

    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidInputException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    fieldName + " is required"
            );
        }
    }
}
