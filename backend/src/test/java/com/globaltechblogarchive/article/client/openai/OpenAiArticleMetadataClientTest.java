package com.globaltechblogarchive.article.client.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataDecision;
import com.globaltechblogarchive.article.application.ArticleMetadataAiClient.ArticleMetadataInput;
import com.globaltechblogarchive.article.exception.ArticleMetadataAiClientException;
import com.globaltechblogarchive.article.domain.ArticleCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiArticleMetadataClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiArticleMetadataClient client = new OpenAiArticleMetadataClient(
            (RestClient) null,
            objectMapper,
            new OpenAiProperties("test-key", "gpt-5-mini")
    );

    @Test
    void modelReturnsConfiguredModel() {
        assertThat(client.model()).isEqualTo("gpt-5-mini");
    }

    @Test
    void buildRequestCreatesResponsesApiStructuredOutputRequest() {
        JsonNode request = client.buildRequest(List.of(
                new ArticleMetadataInput(0, "Scaling APIs"),
                new ArticleMetadataInput(1, "Company launch event")
        ));

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

    @Test
    void parseResponseReturnsDecisionsFromOutputText() {
        String responseBody = """
                {
                  "output": [
                    {
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

        List<ArticleMetadataDecision> decisions = client.parseResponse(responseBody);

        assertThat(decisions).hasSize(1);
        ArticleMetadataDecision decision = decisions.getFirst();
        assertThat(decision.index()).isZero();
        assertThat(decision.translatedTitle()).isEqualTo("Translated API scaling");
        assertThat(decision.category()).isEqualTo(ArticleCategory.BACKEND);
        assertThat(decision.save()).isTrue();
        assertThat(decision.exclusionReason()).isNull();
    }

    @Test
    void parseResponseFailsWhenCategoryIsInvalid() {
        String responseBody = outputText("""
                {"items":[{"index":0,"translatedTitle":"Title","category":"SECURITY","save":true,"exclusionReason":null}]}
                """);

        assertThatThrownBy(() -> client.parseResponse(responseBody))
                .isInstanceOf(ArticleMetadataAiClientException.class)
                .hasMessageContaining("Invalid OpenAI category");
    }

    @Test
    void parseResponseFailsWhenSaveIsMissing() {
        String responseBody = outputText("""
                {"items":[{"index":0,"translatedTitle":"Title","category":"AI","exclusionReason":null}]}
                """);

        assertThatThrownBy(() -> client.parseResponse(responseBody))
                .isInstanceOf(ArticleMetadataAiClientException.class)
                .hasMessageContaining("save");
    }

    private String outputText(String text) {
        return """
                {
                  "output_text": %s
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
