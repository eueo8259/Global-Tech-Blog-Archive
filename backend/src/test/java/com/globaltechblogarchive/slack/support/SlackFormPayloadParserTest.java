package com.globaltechblogarchive.slack.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

class SlackFormPayloadParserTest {

    private final SlackFormPayloadParser parser = new SlackFormPayloadParser();

    @Test
    void parseDecodesFormValuesAndPreservesRepeatedKeys() {
        MultiValueMap<String, String> form = parse(
                "command=%2Fsubscribe&text=java+spring&company=netflix&company=uber&empty="
        );

        assertThat(form.getFirst("command")).isEqualTo("/subscribe");
        assertThat(form.getFirst("text")).isEqualTo("java spring");
        assertThat(form.get("company")).containsExactly("netflix", "uber");
        assertThat(form.getFirst("empty")).isEmpty();
    }

    @Test
    void parseReturnsEmptyFormForEmptyBody() {
        assertThat(parser.parse(new byte[0])).isEmpty();
    }

    @Test
    void parseRejectsMalformedPercentEncoding() {
        assertThatThrownBy(() -> parse("command=%ZZ"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Malformed Slack form payload");
    }

    private MultiValueMap<String, String> parse(String body) {
        return parser.parse(body.getBytes(StandardCharsets.UTF_8));
    }
}
