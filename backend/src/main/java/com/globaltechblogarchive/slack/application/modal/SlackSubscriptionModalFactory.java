package com.globaltechblogarchive.slack.application.modal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaltechblogarchive.slack.application.SlackSubscriptionModalData.CompanyOption;
import com.globaltechblogarchive.slack.application.modal.SlackModalView.InputBlock;
import com.globaltechblogarchive.slack.application.modal.SlackModalView.MultiStaticSelect;
import com.globaltechblogarchive.slack.application.modal.SlackModalView.Option;
import com.globaltechblogarchive.slack.application.modal.SlackModalView.PlainText;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlackSubscriptionModalFactory {

    private final ObjectMapper objectMapper;

    public SlackModalView create(SlackModalMetadata metadata, List<CompanyOption> companies) {
        Set<String> selectedCompanyKeys = companies.stream()
                .filter(CompanyOption::selected)
                .map(CompanyOption::companyKey)
                .collect(Collectors.toSet());
        List<Option> options = companies.stream()
                .map(company -> new Option(plainText(company.companyName()), company.companyKey()))
                .toList();
        List<Option> initialOptions = options.stream()
                .filter(option -> selectedCompanyKeys.contains(option.value()))
                .toList();

        MultiStaticSelect select = new MultiStaticSelect(
                "multi_static_select",
                SlackSubscriptionModalContract.COMPANY_ACTION_ID,
                plainText("Select companies"),
                options,
                initialOptions
        );
        InputBlock input = new InputBlock(
                "input",
                SlackSubscriptionModalContract.COMPANY_BLOCK_ID,
                true,
                plainText("Companies"),
                select
        );

        return new SlackModalView(
                "modal",
                SlackSubscriptionModalContract.CALLBACK_ID,
                plainText("구독할 기업 선택"),
                plainText("Submit"),
                plainText("Cancel"),
                serializeMetadata(metadata),
                List.of(input)
        );
    }

    private String serializeMetadata(SlackModalMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Slack modal metadata", exception);
        }
    }

    private PlainText plainText(String text) {
        return new PlainText("plain_text", text);
    }
}
