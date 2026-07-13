package com.globaltechblogarchive.slack.application.modal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.slack.application.SlackSubscriptionModalData.CompanyOption;
import com.globaltechblogarchive.slack.application.modal.SlackModalView.InputBlock;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlackSubscriptionModalFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SlackSubscriptionModalFactory factory = new SlackSubscriptionModalFactory(objectMapper);

    @Test
    void createBuildsMultiSelectWithExistingSubscriptionsSelected() throws Exception {
        SlackModalMetadata metadata = new SlackModalMetadata("T123", "C123", "tech-news");
        List<CompanyOption> companies = List.of(
                new CompanyOption("airbnb", "Airbnb", true),
                new CompanyOption("netflix", "Netflix", false),
                new CompanyOption("stripe", "Stripe", true)
        );

        SlackModalView view = factory.create(metadata, companies);

        assertThat(view.type()).isEqualTo("modal");
        assertThat(view.callbackId()).isEqualTo(SlackSubscriptionModalContract.CALLBACK_ID);
        assertThat(view.title().text()).isEqualTo("구독할 기업 선택");
        InputBlock input = view.blocks().getFirst();
        assertThat(input.optional()).isTrue();
        assertThat(input.element().type()).isEqualTo("multi_static_select");
        assertThat(input.element().options()).extracting(SlackModalView.Option::value)
                .containsExactly("airbnb", "netflix", "stripe");
        assertThat(input.element().initialOptions()).extracting(SlackModalView.Option::value)
                .containsExactly("airbnb", "stripe");
        SlackModalMetadata serializedMetadata = objectMapper.readValue(
                view.privateMetadata(),
                SlackModalMetadata.class
        );
        assertThat(serializedMetadata).isEqualTo(metadata);
        assertThat(view.privateMetadata()).doesNotContain("U123");
    }

    @Test
    void createOmitsInitialOptionsFromJsonWhenNothingIsSelected() throws Exception {
        SlackModalMetadata metadata = new SlackModalMetadata("T123", "C123", "tech-news");
        List<CompanyOption> companies = List.of(
                new CompanyOption("netflix", "Netflix", false)
        );

        SlackModalView view = factory.create(metadata, companies);

        assertThat(view.blocks().getFirst().element().initialOptions()).isEmpty();
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(view))
                .at("/blocks/0/element/initial_options")
                .isMissingNode()).isTrue();
    }
}
