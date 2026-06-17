package com.globaltechblogarchive.article.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;

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
                throw new IllegalStateException("OpenAI system prompt file not found: " + SYSTEM_PROMPT_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read OpenAI system prompt: " + SYSTEM_PROMPT_PATH, e);
        }
    }

    private JsonNode readResponseFormatSchema() {
        try (InputStream inputStream = getClass().getResourceAsStream(RESPONSE_FORMAT_SCHEMA_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException(
                        "OpenAI response format schema file not found: " + RESPONSE_FORMAT_SCHEMA_PATH
                );
            }
            return objectMapper.readTree(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read OpenAI response format schema", e);
        }
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
