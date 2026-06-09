package com.globaltechblogarchive.article.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OpenAiArticleMetadataResponseParser {

    private final ObjectMapper objectMapper;

    OpenAiArticleMetadataResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<ArticleMetadataDecision> parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String outputText = outputText(root);
            JsonNode parsed = objectMapper.readTree(outputText);
            JsonNode items = parsed.get("items");
            if (items == null || !items.isArray()) {
                throw new ArticleMetadataAiClientException("OpenAI response items must be an array");
            }
            List<ArticleMetadataDecision> decisions = new ArrayList<>();
            for (JsonNode item : items) {
                decisions.add(parseDecision(item));
            }
            return decisions;
        } catch (JsonProcessingException exception) {
            throw new ArticleMetadataAiClientException("Failed to parse OpenAI response", exception);
        }
    }

    private String outputText(JsonNode root) {
        JsonNode directOutputText = root.get("output_text");
        if (directOutputText != null && directOutputText.isTextual()) {
            return directOutputText.asText();
        }

        JsonNode output = root.path("output");
        if (!output.isArray()) {
            throw new ArticleMetadataAiClientException("OpenAI response output must be an array");
        }
        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                JsonNode text = contentItem.get("text");
                if (text != null && text.isTextual()) {
                    return text.asText();
                }
            }
        }
        throw new ArticleMetadataAiClientException("OpenAI response text was not found");
    }

    private ArticleMetadataDecision parseDecision(JsonNode item) {
        require(item, "index");
        require(item, "translatedTitle");
        require(item, "category");
        require(item, "save");
        require(item, "exclusionReason");
        if (!item.get("save").isBoolean()) {
            throw new ArticleMetadataAiClientException("OpenAI response save must be boolean");
        }
        return new ArticleMetadataDecision(
                item.get("index").asInt(),
                item.get("translatedTitle").asText(),
                category(item.get("category").asText()),
                item.get("save").asBoolean(),
                optionalText(item.get("exclusionReason"))
        );
    }

    private void require(JsonNode item, String fieldName) {
        boolean missing = !item.has(fieldName);
        boolean invalidNull = item.has(fieldName)
                && item.get(fieldName).isNull()
                && !"exclusionReason".equals(fieldName);
        if (missing || invalidNull) {
            throw new ArticleMetadataAiClientException("OpenAI response missing field: " + fieldName);
        }
    }

    private ArticleCategory category(String value) {
        try {
            return ArticleCategory.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ArticleMetadataAiClientException("Invalid OpenAI category: " + value, exception);
        }
    }

    private String optionalText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
