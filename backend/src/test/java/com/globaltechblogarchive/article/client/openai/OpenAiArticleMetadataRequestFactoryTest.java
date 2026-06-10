package com.globaltechblogarchive.article.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenAiArticleMetadataRequestFactoryTest {

    private final OpenAiArticleMetadataRequestFactory requestFactory =
            new OpenAiArticleMetadataRequestFactory(new ObjectMapper());

    @Test
    void createBuildsResponsesApiStructuredOutputRequest() {
        JsonNode request = requestFactory.create(
                List.of(
                        new ArticleMetadataInput(0, "Scaling APIs"),
                        new ArticleMetadataInput(1, "Company launch event")
                ),
                "gpt-5-mini"
        );

        assertThat(request.path("model").asText()).isEqualTo("gpt-5-mini");
        assertThat(request.path("input")).hasSize(2);
        assertThat(request.path("input").get(0).path("role").asText()).isEqualTo("system");
        assertThat(request.path("input").get(0).path("content").asText())
                .contains("Do not save an article just because it mentions AI")
                .contains("\"Introducing GPT-Rosalind\" -> category=ELSE, save=false")
                .contains("If save=false, category must be ELSE");
        assertThat(request.path("input").get(1).path("content").asText())
                .contains("\"index\":0")
                .contains("\"title\":\"Scaling APIs\"");

        JsonNode format = request.path("text").path("format");
        assertThat(format.path("type").asText()).isEqualTo("json_schema");
        assertThat(format.path("name").asText()).isEqualTo("article_metadata_decisions");
        assertThat(format.path("strict").asBoolean()).isTrue();
        JsonNode item = format.path("schema")
                .path("properties")
                .path("items")
                .path("items");
        assertThat(item.path("required")).extracting(JsonNode::asText)
                .containsExactly("index", "translatedTitle", "category", "save", "exclusionReason");
        assertThat(item.path("properties").path("category").path("enum")).extracting(JsonNode::asText)
                .containsExactly("FRONTEND", "BACKEND", "DEVOPS", "ARCHITECTURE", "AI", "ELSE");
    }
}
