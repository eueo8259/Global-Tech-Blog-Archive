package com.globaltechblogarchive.article.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import com.globaltechblogarchive.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OpenAiArticleMetadataResponseParser {

    private final ObjectMapper objectMapper;

    List<ArticleMetadataDecision> parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = extractItems(root);
            return parseDecisions(items);
        } catch (JsonProcessingException exception) {
            throw new ArticleMetadataAiClientException(
                    ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR,
                    "Failed to parse OpenAI response"
            );
        }
    }

    private JsonNode extractItems(JsonNode root) throws JsonProcessingException {
        String outputText = extractOutputText(root);
        JsonNode parsed = objectMapper.readTree(outputText);
        JsonNode items = parsed.get("items");
        if (items == null || !items.isArray()) {
            throw aiClientException("OpenAI response items must be an array");
        }
        return items;
    }

    private String extractOutputText(JsonNode root) {
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            throw aiClientException("OpenAI response output must be an array");
        }

        for (JsonNode outputItem : output) {
            if (!"message".equals(outputItem.path("type").asText())) {
                continue;
            }
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if (!"output_text".equals(contentItem.path("type").asText())) {
                    continue;
                }
                JsonNode text = contentItem.get("text");
                if (text != null && text.isTextual()) {
                    return text.asText();
                }
            }
        }
        throw aiClientException("OpenAI response text was not found");
    }

    private List<ArticleMetadataDecision> parseDecisions(JsonNode items) {
        List<ArticleMetadataDecision> decisions = new ArrayList<>();
        for (JsonNode item : items) {
            decisions.add(parseDecision(item));
        }
        return decisions;
    }

    private ArticleMetadataDecision parseDecision(JsonNode item) {
        return new ArticleMetadataDecision(
                requiredInt(item, "index"),
                requiredText(item, "translatedTitle"),
                category(requiredText(item, "category")),
                requiredBoolean(item, "save"),
                nullableText(item, "exclusionReason")
        );
    }

    private JsonNode required(JsonNode item, String fieldName) {
        JsonNode value = item.get(fieldName);
        if (value == null || value.isNull()) {
            throw aiClientException("OpenAI response missing field: " + fieldName);
        }
        return value;
    }

    private int requiredInt(JsonNode item, String fieldName) {
        JsonNode value = required(item, fieldName);
        if (!value.isInt()) {
            throw aiClientException("OpenAI response field must be integer: " + fieldName);
        }
        return value.asInt();
    }

    private String requiredText(JsonNode item, String fieldName) {
        JsonNode value = required(item, fieldName);
        if (!value.isTextual()) {
            throw aiClientException("OpenAI response field must be text: " + fieldName);
        }
        return value.asText();
    }

    private ArticleCategory category(String value) {
        try {
            return ArticleCategory.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ArticleMetadataAiClientException(
                    ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR,
                    "Invalid OpenAI category: " + value
            );
        }
    }

    private boolean requiredBoolean(JsonNode item, String fieldName) {
        JsonNode value = required(item, fieldName);
        if (!value.isBoolean()) {
            throw aiClientException("OpenAI response field must be boolean: " + fieldName);
        }
        return value.asBoolean();
    }

    private String nullableText(JsonNode item, String fieldName) {
        JsonNode value = item.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw aiClientException("OpenAI response field must be text or null: " + fieldName);
        }
        return value.asText();
    }

    private ArticleMetadataAiClientException aiClientException(String message) {
        return new ArticleMetadataAiClientException(ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR, message);
    }
}
