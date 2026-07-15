package com.globaltechblogarchive.slack.application.digest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackDigestMessageFactoryTest {

    private final SlackDigestMessageFactory factory = new SlackDigestMessageFactory();

    @Test
    void createGroupsArticlesByCompanyInOneChannelMessage() {
        List<SlackDigestArticle> articles = List.of(
                article(1L, 1L, "Netflix", "Netflix 1"),
                article(2L, 1L, "Netflix", "Netflix 2"),
                article(3L, 2L, "Uber", "Uber 1")
        );

        SlackChatMessage message = factory.create("C123", articles);

        assertThat(message.channel()).isEqualTo("C123");
        assertThat(message.text()).contains("3개");
        assertThat(message.blocks()).hasSize(3);
        assertThat(message.blocks().get(1).text().text())
                .contains("*Netflix*", "Netflix 1", "Netflix 2");
        assertThat(message.blocks().get(2).text().text())
                .contains("*Uber*", "Uber 1");
    }

    @Test
    void createLimitsArticleCountAndAddsOmittedContext() {
        List<SlackDigestArticle> articles = new ArrayList<>();
        for (long id = 1; id <= 31; id++) {
            articles.add(article(id, 1L, "Netflix", "Article " + id));
        }

        SlackChatMessage message = factory.create("C123", articles);

        assertThat(message.text()).contains("30개");
        assertThat(message.blocks().getLast().elements().getFirst().text()).contains("1개");
    }

    @Test
    void createEscapesSlackMrkdwnControlCharacters() {
        SlackChatMessage message = factory.create(
                "C123",
                List.of(article(1L, 1L, "A&B", "Use <Java> & Spring"))
        );

        assertThat(message.blocks().get(1).text().text())
                .contains("A&amp;B", "Use &lt;Java&gt; &amp; Spring");
    }

    private SlackDigestArticle article(Long id, Long companyId, String companyName, String title) {
        return new SlackDigestArticle(
                id,
                companyId,
                companyName,
                title,
                "https://example.com/" + id,
                LocalDateTime.of(2026, 7, 14, 4, 0)
        );
    }
}
