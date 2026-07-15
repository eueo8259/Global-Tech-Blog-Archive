package com.globaltechblogarchive.slack.application.digest;

import com.globaltechblogarchive.slack.application.digest.SlackChatMessage.Block;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SlackDigestMessageFactory {

    static final int MAX_ARTICLE_COUNT = 30;
    static final int MAX_TITLE_LENGTH = 300;
    static final int MAX_SECTION_LENGTH = 2800;

    public SlackChatMessage create(String slackChannelId, List<SlackDigestArticle> articles) {
        if (articles.isEmpty()) {
            throw new IllegalArgumentException("Daily Digest에 포함할 아티클이 필요합니다.");
        }

        List<SlackDigestArticle> includedArticles = articles.stream()
                .limit(MAX_ARTICLE_COUNT)
                .toList();
        Map<String, List<SlackDigestArticle>> articlesByCompany = includedArticles.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        SlackDigestArticle::companyName,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        List<Block> blocks = new ArrayList<>();
        blocks.add(Block.header("오늘의 새로운 기술 아티클 " + includedArticles.size() + "개"));
        for (Map.Entry<String, List<SlackDigestArticle>> entry : articlesByCompany.entrySet()) {
            blocks.addAll(companyBlocks(entry.getKey(), entry.getValue()));
        }

        int omittedCount = articles.size() - includedArticles.size();
        if (omittedCount > 0) {
            blocks.add(Block.context("메시지 제한으로 " + omittedCount + "개 아티클을 생략했습니다."));
        }

        String fallbackText = "오늘의 새로운 기술 아티클 " + includedArticles.size() + "개";
        return new SlackChatMessage(slackChannelId, fallbackText, List.copyOf(blocks));
    }

    private List<Block> companyBlocks(String companyName, List<SlackDigestArticle> articles) {
        List<Block> blocks = new ArrayList<>();
        String companyHeader = "*" + escape(companyName) + "*";
        StringBuilder section = new StringBuilder(companyHeader);
        for (SlackDigestArticle article : articles) {
            String line = "\n• <" + article.articleUrl() + "|" + displayTitle(article.title()) + ">";
            if (section.length() + line.length() > MAX_SECTION_LENGTH) {
                blocks.add(Block.section(section.toString()));
                section = new StringBuilder(companyHeader);
            }
            section.append(line);
        }
        blocks.add(Block.section(section.toString()));
        return blocks;
    }

    private String displayTitle(String title) {
        String escapedTitle = escape(title);
        if (escapedTitle.length() <= MAX_TITLE_LENGTH) {
            return escapedTitle;
        }
        return escapedTitle.substring(0, MAX_TITLE_LENGTH - 1) + "…";
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
