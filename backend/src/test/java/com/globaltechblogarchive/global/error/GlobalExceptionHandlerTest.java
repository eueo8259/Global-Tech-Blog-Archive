package com.globaltechblogarchive.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleHttpMediaTypeNotSupportedExceptionReturnsUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
                MediaType.APPLICATION_JSON,
                java.util.List.of(MediaType.APPLICATION_FORM_URLENCODED)
        );

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleHttpMediaTypeNotSupportedException(exception);

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(415);
        org.assertj.core.api.Assertions.assertThat(response.getBody()).isEqualTo(
                ErrorResponse.from(ErrorCode.UNSUPPORTED_MEDIA_TYPE)
        );
    }

    @Test
    void handleBusinessExceptionReturnsErrorCodeResponse() {
        ArticleMetadataAiClientException exception = new ArticleMetadataAiClientException(
                ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR,
                "OpenAI response items must be an array"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR.getStatus());
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(
                "A001",
                "OpenAI response items must be an array"
        ));
    }
}
