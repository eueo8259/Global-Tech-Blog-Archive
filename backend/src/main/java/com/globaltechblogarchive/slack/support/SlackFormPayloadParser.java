package com.globaltechblogarchive.slack.support;

import com.globaltechblogarchive.global.error.ErrorCode;
import com.globaltechblogarchive.global.error.exception.InvalidInputException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
public class SlackFormPayloadParser {

    public MultiValueMap<String, String> parse(byte[] rawBody) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (rawBody == null || rawBody.length == 0) {
            return form;
        }

        String body = new String(rawBody, StandardCharsets.UTF_8);
        try {
            for (String pair : body.split("&", -1)) {
                if (pair.isEmpty()) {
                    continue;
                }
                int separatorIndex = pair.indexOf('=');
                String encodedKey = pair;
                String encodedValue = "";
                if (separatorIndex >= 0) {
                    encodedKey = pair.substring(0, separatorIndex);
                    encodedValue = pair.substring(separatorIndex + 1);
                }
                form.add(decode(encodedKey), decode(encodedValue));
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE, "Malformed Slack form payload");
        }
        return form;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
