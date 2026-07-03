package com.globaltechblogarchive.article.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import com.globaltechblogarchive.global.error.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OpenAiArticleMetadataRequestFactory {

    private static final String SYSTEM_PROMPT_PATH = "/openai/article-metadata-system-prompt.md";
    private static final String RESPONSE_FORMAT_SCHEMA_PATH = "/openai/article-metadata-response-format.json";

    private final ObjectMapper objectMapper;
    private final String systemPrompt;
    private final JsonNode responseFormatSchema;

    OpenAiArticleMetadataRequestFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.systemPrompt = readSystemPrompt();
        this.responseFormatSchema = readResponseFormatSchema();
    }

    JsonNode create(List<ArticleMetadataInput> inputs, String model) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.set("input", inputMessages(inputs));
        request.set("text", responseTextFormat());
        return request;
    }

    private String readSystemPrompt() {
        try (InputStream inputStream = getClass().getResourceAsStream(SYSTEM_PROMPT_PATH)) {
            if (inputStream == null) {
                throw aiClientException("OpenAI system prompt file not found: " + SYSTEM_PROMPT_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw aiClientException("Failed to read OpenAI system prompt: " + SYSTEM_PROMPT_PATH, e);
        }
    }

    private JsonNode readResponseFormatSchema() {
        try (InputStream inputStream = getClass().getResourceAsStream(RESPONSE_FORMAT_SCHEMA_PATH)) {
            if (inputStream == null) {
                throw aiClientException(
                        "OpenAI response format schema file not found: " + RESPONSE_FORMAT_SCHEMA_PATH
                );
            }
            return objectMapper.readTree(inputStream);
        } catch (IOException e) {
            throw aiClientException("Failed to read OpenAI response format schema", e);
        }
    }

    private ArticleMetadataAiClientException aiClientException(String message) {
        return new ArticleMetadataAiClientException(ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR, message);
    }

    private ArticleMetadataAiClientException aiClientException(String message, Throwable cause) {
        return new ArticleMetadataAiClientException(ErrorCode.ARTICLE_METADATA_AI_CLIENT_ERROR, message, cause);
    }

    private ArrayNode inputMessages(List<ArticleMetadataInput> inputs) {
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt(inputs)));
        return messages;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String userPrompt(List<ArticleMetadataInput> inputs) {
        ArrayNode items = objectMapper.createArrayNode();
        for (ArticleMetadataInput input : inputs) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("index", input.index());
            item.put("title", input.title());
            item.put("shortContext", input.shortContext());
            if (input.categoryHint() == null || input.categoryHint().isBlank()) {
                item.putNull("categoryHint");
            } else {
                item.put("categoryHint", input.categoryHint());
            }
            items.add(item);
        }
        return "Classify these article candidates: " + items;
    }

    private ObjectNode responseTextFormat() {
        ObjectNode text = objectMapper.createObjectNode();
        text.set("format", responseFormatSchema);
        return text;
    }
}
