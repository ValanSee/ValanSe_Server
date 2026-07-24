package com.valanse.valanse.common.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DiscordWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(DiscordWebhookClient.class);
    private static final int DESCRIPTION_LIMIT = 3_000;

    private final RestClient restClient;
    private final DiscordAlertProperties properties;
    private final Sleeper sleeper;

    public DiscordWebhookClient(RestClient restClient, DiscordAlertProperties properties) {
        this(restClient, properties, duration -> Thread.sleep(duration.toMillis()));
    }

    DiscordWebhookClient(RestClient restClient, DiscordAlertProperties properties, Sleeper sleeper) {
        this.restClient = restClient;
        this.properties = properties;
        this.sleeper = sleeper;
    }

    public void send(ServerErrorEvent event) {
        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            throw new IllegalStateException("Discord webhook URL is not configured.");
        }

        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                sendOnce(event);
                return;
            } catch (RestClientResponseException exception) {
                if (!isRetryableStatus(exception) || attempt == maxAttempts) {
                    throw exception;
                }
                Duration delay = retryDelay(exception, attempt);
                logRetry(event, attempt, maxAttempts, delay, "HTTP " + exception.getStatusCode().value());
                sleep(delay);
            } catch (ResourceAccessException exception) {
                if (attempt == maxAttempts) {
                    throw exception;
                }
                Duration delay = exponentialBackoff(attempt);
                logRetry(event, attempt, maxAttempts, delay, exception.getClass().getSimpleName());
                sleep(delay);
            }
        }
    }

    private void sendOnce(ServerErrorEvent event) {
        restClient.post()
                .uri(webhookUri())
                .body(payload(event))
                .retrieve()
                .toBodilessEntity();
    }

    private boolean isRetryableStatus(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return status == 429 || exception.getStatusCode().is5xxServerError();
    }

    private Duration retryDelay(RestClientResponseException exception, int attempt) {
        if (exception.getStatusCode().value() == 429) {
            return retryAfter(exception.getResponseHeaders()).orElseGet(() -> exponentialBackoff(attempt));
        }
        return exponentialBackoff(attempt);
    }

    private Optional<Duration> retryAfter(HttpHeaders headers) {
        if (headers == null) {
            return Optional.empty();
        }

        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }

        try {
            double seconds = Double.parseDouble(value);
            if (!Double.isFinite(seconds) || seconds < 0) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofMillis(Math.max(1L, (long) Math.ceil(seconds * 1_000))));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private Duration exponentialBackoff(int attempt) {
        long initialMillis = Math.max(0L, properties.getInitialBackoff().toMillis());
        long maxMillis = Math.max(initialMillis, properties.getMaxBackoff().toMillis());
        long multiplier = 1L << Math.min(attempt - 1, 30);

        long delayMillis;
        try {
            delayMillis = Math.multiplyExact(initialMillis, multiplier);
        } catch (ArithmeticException ignored) {
            delayMillis = maxMillis;
        }
        return Duration.ofMillis(Math.min(delayMillis, maxMillis));
    }

    private void logRetry(
            ServerErrorEvent event,
            int attempt,
            int maxAttempts,
            Duration delay,
            String failure
    ) {
        log.warn(
                "Retrying Discord server error alert. traceId={}, nextAttempt={}/{}, delayMs={}, failure={}",
                event.traceId(),
                attempt + 1,
                maxAttempts,
                delay.toMillis(),
                failure
        );
    }

    private void sleep(Duration delay) {
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Discord alert retry was interrupted.", exception);
        }
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

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }
}
