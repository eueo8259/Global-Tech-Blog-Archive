package com.globaltechblogarchive.article.client.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OpenAiArticleMetadataRequestFactory {

    private final ObjectMapper objectMapper;

    OpenAiArticleMetadataRequestFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    JsonNode create(List<ArticleMetadataInput> inputs, String model) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.set("input", inputMessages(inputs));
        ObjectNode text = request.putObject("text");
        text.set("format", responseFormatSchema());
        return request;
    }

    private ArrayNode inputMessages(List<ArticleMetadataInput> inputs) {
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(message("system", systemPrompt()));
        messages.add(message("user", userPrompt(inputs)));
        return messages;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String systemPrompt() {
        return """
                You decide whether engineering blog article titles should be saved for an MVP archive of engineering-team technical articles.

                Input contains only article titles.

                Primary goal:
                Save articles only when the title suggests meaningful engineering content for developers, such as implementation details, architecture, infrastructure, reliability, performance, security, developer experience, technical lessons, AI engineering, or operational practice.

                Decision order:
                1. First decide save=true or save=false.
                2. If save=false, category must be ELSE.
                3. If save=true, choose exactly one category:
                   FRONTEND, BACKEND, DEVOPS, ARCHITECTURE, AI.

                Important exclusion rules:
                Do not save titles that look like product announcements, feature launches, company news, event announcements, hiring posts, marketing posts, customer stories, partnerships, funding/news updates, or general availability announcements.

                AI-specific rule:
                Do not save an article just because it mentions AI, GPT, LLM, model, agent, assistant, benchmark, release, or a new AI feature.
                Classify as AI and save=true only when the title suggests engineering work around AI systems, such as building, deploying, evaluating, operating, optimizing, scaling, monitoring, or integrating AI systems.

                If a title is about a new AI product, model, feature, benchmark result, launch, availability, or official announcement without clear engineering implementation detail:
                category=ELSE
                save=false

                Category definitions:
                FRONTEND:
                Browser, UI, client-side architecture, React/Vue/Angular, design systems, web performance, accessibility.

                BACKEND:
                APIs, services, databases, distributed systems, server-side application logic, data processing.

                DEVOPS:
                Infrastructure, deployment, CI/CD, observability, SRE, reliability, incident response, cloud operations.

                ARCHITECTURE:
                System design, large-scale architecture, platform architecture, technical tradeoffs across multiple systems.

                AI:
                AI engineering only. Model serving, inference systems, eval pipelines, RAG, agents as engineered systems, ML infrastructure, AI product implementation details.

                ELSE:
                Non-engineering content, product/company/event/marketing/news/release content, or unclear technical value.

                Examples:
                - "Introducing GPT-Rosalind" -> category=ELSE, save=false
                - "New GPT-Rosalind features for developers" -> category=ELSE, save=false
                - "GPT-Rosalind is now available in the API" -> category=ELSE, save=false
                - "GPT-Rosalind benchmark results" -> category=ELSE, save=false
                - "How we scaled GPT-Rosalind inference" -> category=AI, save=true
                - "Building the evaluation pipeline for GPT-Rosalind" -> category=AI, save=true
                - "Lessons from operating GPT-Rosalind in production" -> category=AI, save=true
                - "Reducing latency in our recommendation service" -> category=BACKEND, save=true
                - "Our company at Developer Summit 2026" -> category=ELSE, save=false

                Output must follow the provided JSON schema exactly.
                Translate saved and rejected titles into Korean in translatedTitle.
                """;
    }

    private String userPrompt(List<ArticleMetadataInput> inputs) {
        ArrayNode items = objectMapper.createArrayNode();
        for (ArticleMetadataInput input : inputs) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("index", input.index());
            item.put("title", input.title());
            items.add(item);
        }
        return "Classify these article titles: " + items;
    }

    private ObjectNode responseFormatSchema() {
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", "article_metadata_decisions");
        format.put("strict", true);

        ObjectNode schema = format.putObject("schema");
        schema.put("type", "object");
        schema.set("required", array("items"));
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        ObjectNode items = properties.putObject("items");
        items.put("type", "array");
        ObjectNode item = items.putObject("items");
        item.put("type", "object");
        item.set("required", array("index", "translatedTitle", "category", "save", "exclusionReason"));
        item.put("additionalProperties", false);

        ObjectNode itemProperties = item.putObject("properties");
        itemProperties.putObject("index").put("type", "integer");
        itemProperties.putObject("translatedTitle").put("type", "string");
        itemProperties.putObject("category")
                .put("type", "string")
                .set("enum", array("FRONTEND", "BACKEND", "DEVOPS", "ARCHITECTURE", "AI", "ELSE"));
        itemProperties.putObject("save").put("type", "boolean");
        itemProperties.putObject("exclusionReason")
                .set("enum", array(
                        null,
                        "PRODUCT_NEWS",
                        "COMPANY_NEWS",
                        "EVENT",
                        "HIRING",
                        "MARKETING",
                        "LOW_TECHNICAL_SIGNAL",
                        "NOT_ENGINEERING",
                        "OTHER"
                ));

        return format;
    }

    private ArrayNode array(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            if (value == null) {
                array.addNull();
            } else {
                array.add(value);
            }
        }
        return array;
    }
}
