package com.valanse.valanse.common.alert;

import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class DiscordWebhookClient {

    private static final int DESCRIPTION_LIMIT = 3_000;

    private final RestClient restClient;
    private final DiscordAlertProperties properties;

    public DiscordWebhookClient(RestClient restClient, DiscordAlertProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public void send(ServerErrorEvent event) {
        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            throw new IllegalStateException("Discord webhook URL is not configured.");
        }

        restClient.post()
                .uri(webhookUri())
                .body(payload(event))
                .retrieve()
                .toBodilessEntity();
    }

    private String webhookUri() {
        return properties.getWebhookUrl() + (properties.getWebhookUrl().contains("?") ? "&wait=true" : "?wait=true");
    }

    private Map<String, Object> payload(ServerErrorEvent event) {
        Map<String, Object> embed = Map.of(
                "title", "Server " + event.status() + " Error",
                "color", 0xE74C3C,
                "description", stackTrace(event),
                "fields", List.of(
                        field("Environment", event.environment(), true),
                        field("Endpoint", event.httpMethod() + " " + event.requestUri(), false),
                        field("Exception", event.exceptionType() + ": " + safe(event.exceptionMessage()), false),
                        field("Trace ID", event.traceId(), false)
                ),
                "timestamp", event.occurredAt().toString()
        );

        return Map.of(
                "username", "ValanSe Error Alert",
                "embeds", List.of(embed),
                "allowed_mentions", Map.of("parse", List.of())
        );
    }

    private Map<String, Object> field(String name, String value, boolean inline) {
        return Map.of(
                "name", name,
                "value", truncate(safe(value), 1_024),
                "inline", inline
        );
    }

    private String stackTrace(ServerErrorEvent event) {
        StringBuilder value = new StringBuilder();
        value.append(event.exceptionType()).append(": ").append(safe(event.exceptionMessage())).append('\n');

        for (StackTraceElement element : event.cause().getStackTrace()) {
            value.append("at ").append(element).append('\n');
            if (value.length() >= DESCRIPTION_LIMIT) {
                break;
            }
        }

        return "```\n" + truncate(value.toString(), DESCRIPTION_LIMIT - 9) + "\n```";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "(no message)";
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
