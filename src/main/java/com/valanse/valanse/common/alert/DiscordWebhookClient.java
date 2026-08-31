package com.valanse.valanse.common.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        sendWithRetry(event.traceId(), payload(event));
    }

    public void send(ContentSeedRunEvent event) {
        sendWithRetry(correlationId(event), payload(event));
    }

    private void sendWithRetry(String correlationId, Map<String, Object> payload) {
        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            throw new IllegalStateException("Discord webhook URL is not configured.");
        }

        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                sendOnce(payload);
                return;
            } catch (RestClientResponseException exception) {
                if (!isRetryableStatus(exception) || attempt == maxAttempts) {
                    throw exception;
                }
                Duration delay = retryDelay(exception, attempt);
                logRetry(correlationId, attempt, maxAttempts, delay, "HTTP " + exception.getStatusCode().value());
                sleep(delay);
            } catch (ResourceAccessException exception) {
                if (attempt == maxAttempts) {
                    throw exception;
                }
                Duration delay = exponentialBackoff(attempt);
                logRetry(correlationId, attempt, maxAttempts, delay, exception.getClass().getSimpleName());
                sleep(delay);
            }
        }
    }

    private void sendOnce(Map<String, Object> payload) {
        restClient.post()
                .uri(webhookUri())
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private String correlationId(ContentSeedRunEvent event) {
        return "content-seed:" + event.trigger() + ":" + event.occurredAt();
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
            String correlationId,
            int attempt,
            int maxAttempts,
            Duration delay,
            String failure
    ) {
        log.warn(
                "Retrying Discord alert. correlationId={}, nextAttempt={}/{}, delayMs={}, failure={}",
                correlationId,
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

    private Map<String, Object> payload(ContentSeedRunEvent event) {
        Map<String, Object> embed = event.isFatal() ? fatalEmbed(event) : summaryEmbed(event);
        return Map.of(
                "username", "ValanSe Content Seed",
                "embeds", List.of(embed),
                "allowed_mentions", Map.of("parse", List.of())
        );
    }

    private Map<String, Object> fatalEmbed(ContentSeedRunEvent event) {
        return Map.of(
                "title", "콘텐츠 시드 실행 실패",
                "color", 0xE74C3C,
                "fields", List.of(
                        field("트리거", event.trigger(), true),
                        field("환경", event.environment(), true),
                        field("오류", safe(event.fatalErrorType()) + ": " + safe(event.fatalErrorMessage()), false),
                        field("소요 시간", durationText(event.durationMs()), true)
                ),
                "timestamp", event.occurredAt().toString()
        );
    }

    private Map<String, Object> summaryEmbed(ContentSeedRunEvent event) {
        var result = event.result();
        boolean fullySaved = result.savedPostCount() >= result.targetPostCount()
                && result.savedInteractionCount() >= result.targetInteractionCount();

        List<Map<String, Object>> fields = new ArrayList<>(List.of(
                field("트리거", event.trigger(), true),
                field("환경", event.environment(), true),
                field("게시글", result.savedPostCount() + "/" + result.targetPostCount(), true),
                field("상호작용", result.savedInteractionCount() + "/" + result.targetInteractionCount(), true),
                field("API 호출", result.usage().apiCallCount() + "회", true),
                field("토큰 사용량", "입력 " + result.usage().inputTokens() + " · 출력 " + result.usage().outputTokens(), true),
                field("예상 비용", "$" + result.usage().estimatedCostUsd(), true),
                field("소요 시간", durationText(event.durationMs()), true)
        ));

        String links = postLinks(event);
        if (StringUtils.hasText(links)) {
            fields.add(field("생성된 게시글", links, false));
        }

        String rejectionSummary = rejectionSummary(result.rejectionReasonCounts());
        if (StringUtils.hasText(rejectionSummary)) {
            fields.add(field("품질 거절 사유", rejectionSummary, false));
        }

        return Map.of(
                "title", fullySaved ? "콘텐츠 시드 실행 완료" : "콘텐츠 시드 실행 부분 완료",
                "color", fullySaved ? 0x2ECC71 : 0xF1C40F,
                "fields", fields,
                "timestamp", event.occurredAt().toString()
        );
    }

    private String postLinks(ContentSeedRunEvent event) {
        if (event.result().savedPostIds().isEmpty() || !StringUtils.hasText(event.frontendBaseUrl())) {
            return "";
        }
        return event.result().savedPostIds().stream()
                .map(id -> event.frontendBaseUrl() + "/votes/" + id)
                .collect(Collectors.joining("\n"));
    }

    private String rejectionSummary(Map<String, Long> rejectionReasonCounts) {
        return rejectionReasonCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> entry.getKey() + " (" + entry.getValue() + "건)")
                .collect(Collectors.joining("\n"));
    }

    private String durationText(long durationMs) {
        return (durationMs / 1000) + "." + (durationMs % 1000 / 100) + "초";
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
