package com.globaltechblogarchive.article.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import com.globaltechblogarchive.global.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenAiArticleMetadataResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiArticleMetadataResponseParser responseParser =
            new OpenAiArticleMetadataResponseParser(objectMapper);

    @Test
    void parseReturnsDecisionsFromOutputContentText() {
        String responseBody = """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"items\\":[{\\"index\\":0,\\"translatedTitle\\":\\"Translated API scaling\\",\\"category\\":\\"BACKEND\\",\\"save\\":true,\\"exclusionReason\\":null}]}"
                        }
                      ]
                    }
                  ]
                }
                """;

        List<ArticleMetadataDecision> decisions = responseParser.parse(responseBody);

        assertThat(decisions).hasSize(1);
        ArticleMetadataDecision decision = decisions.getFirst();
        assertThat(decision.index()).isZero();
        assertThat(decision.translatedTitle()).isEqualTo("Translated API scaling");
        assertThat(decision.category()).isEqualTo(ArticleCategory.BACKEND);
        assertThat(decision.save()).isTrue();
        assertThat(decision.exclusionReason()).isNull();
    }

    @Test
    void parseReturnsDecisionsFromDirectOutputText() {
        String responseBody = outputText("""
                {"items":[{"index":0,"translatedTitle":"AI systems","category":"AI","save":true,"exclusionReason":null}]}
                """);

        List<ArticleMetadataDecision> decisions = responseParser.parse(responseBody);

        assertThat(decisions).extracting(ArticleMetadataDecision::category)
                .containsExactly(ArticleCategory.AI);
    }

    @Test
    void parseFailsWhenCategoryIsInvalid() {
        String responseBody = outputText("""
                {"items":[{"index":0,"translatedTitle":"Title","category":"SECURITY","save":true,"exclusionReason":null}]}
                """);

        assertThatThrownBy(() -> responseParser.parse(responseBody))
                .isInstanceOfSatisfying(ArticleMetadataAiClientException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR);
                    assertThat(exception).hasMessageContaining("Invalid OpenAI category");
                });
    }

    @Test
    void parseFailsWhenSaveIsMissing() {
        String responseBody = outputText("""
                {"items":[{"index":0,"translatedTitle":"Title","category":"AI","exclusionReason":null}]}
                """);

        assertThatThrownBy(() -> responseParser.parse(responseBody))
                .isInstanceOfSatisfying(ArticleMetadataAiClientException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR);
                    assertThat(exception).hasMessageContaining("save");
                });
    }

    private String outputText(String text) {
        return """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(toJsonString(text.strip()));
    }

    private String toJsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
