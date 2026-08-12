package com.globaltechblogarchive.slack.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;

class SlackPropertiesTest {

    private static final String APPLICATION_YML = "application.yml";

    @Test
    void bindsSlackPropertiesFromEnvironmentVariables() throws IOException {
        StandardEnvironment environment = environmentWithSlackVariables(Map.of(
                "SLACK_CLIENT_ID", "client-id",
                "SLACK_CLIENT_SECRET", "client-secret",
                "SLACK_SIGNING_SECRET", "signing-secret",
                "SLACK_REDIRECT_URI", "http://localhost:8080/api/slack/oauth/callback",
                "SLACK_BOT_SCOPES", "commands,chat:write,channels:read",
                "SLACK_SETTINGS_BASE_URL", "https://techport.example.com/slack/settings"
        ));

        SlackProperties properties = bind(environment);

        assertThat(properties.clientId()).isEqualTo("client-id");
        assertThat(properties.clientSecret()).isEqualTo("client-secret");
        assertThat(properties.signingSecret()).isEqualTo("signing-secret");
        assertThat(properties.redirectUri()).isEqualTo("http://localhost:8080/api/slack/oauth/callback");
        assertThat(properties.botScopes()).isEqualTo("commands,chat:write,channels:read");
        assertThat(properties.settingsBaseUrl()).isEqualTo("https://techport.example.com/slack/settings");
    }

    @Test
    void bindsLocalDefaultsWhenSlackEnvironmentVariablesAreEmpty() throws IOException {
        StandardEnvironment environment = environmentWithSlackVariables(Map.of());

        SlackProperties properties = bind(environment);

        assertThat(properties.clientId()).isEmpty();
        assertThat(properties.clientSecret()).isEmpty();
        assertThat(properties.signingSecret()).isEmpty();
        assertThat(properties.redirectUri()).isEmpty();
        assertThat(properties.botScopes())
                .isEqualTo("commands,chat:write,channels:history,groups:history,metadata.message:read");
        assertThat(properties.settingsBaseUrl()).isEqualTo("http://localhost:5173");
    }

    @Test
    void bindsDailyDigestVerificationDefaults() throws IOException {
        StandardEnvironment environment = environmentWithSlackVariables(Map.of());

        SlackDailyDigestProperties properties = Binder.get(environment)
                .bind("slack.daily-digest", SlackDailyDigestProperties.class)
                .orElseThrow(() -> new IllegalStateException("daily digest properties must bind"));

        assertThat(properties.verificationDelay()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.verificationPermissionErrorDelay()).isEqualTo(Duration.ofHours(1));
        assertThat(properties.maxVerificationChecks()).isEqualTo(3);
        assertThat(properties.historyLookback()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.historyPageSize()).isEqualTo(15);
    }

    private static SlackProperties bind(StandardEnvironment environment) {
        return Binder.get(environment)
                .bind("slack", SlackProperties.class)
                .orElseThrow(() -> new IllegalStateException("slack properties must bind"));
    }

    private static StandardEnvironment environmentWithSlackVariables(Map<String, Object> variables) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        propertySources.addFirst(new SystemEnvironmentPropertySource("testSlackEnvironment", variables));
        loadApplicationYml().forEach(propertySources::addLast);
        return environment;
    }

    private static List<PropertySource<?>> loadApplicationYml() throws IOException {
        return new YamlPropertySourceLoader().load(
                "applicationConfig: [classpath:/" + APPLICATION_YML + "]",
                new ClassPathResource(APPLICATION_YML)
        );
    }
}
