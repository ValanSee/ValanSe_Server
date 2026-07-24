package com.valanse.valanse.common.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class DiscordAlertDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(DiscordAlertDeduplicator.class);
    private static final String KEY_PREFIX = "alert:discord:dedupe:";
    private static final Pattern UUID_PATH_SEGMENT = Pattern.compile(
            "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)"
    );
    private static final Pattern NUMERIC_PATH_SEGMENT = Pattern.compile("/\\d+(?=/|$)");
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final DiscordAlertProperties properties;

    public DiscordAlertDeduplicator(
            StringRedisTemplate redisTemplate,
            DiscordAlertProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public boolean shouldSend(ServerErrorEvent event) {
        if (!properties.isDeduplicationEnabled()) {
            return true;
        }

        try {
            Boolean firstOccurrence = redisTemplate.opsForValue().setIfAbsent(
                    key(event),
                    event.traceId(),
                    properties.getDeduplicationWindow()
            );
            return !Boolean.FALSE.equals(firstOccurrence);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to check Discord alert duplication; allowing alert. traceId={}, failureType={}",
                    event.traceId(),
                    exception.getClass().getSimpleName()
            );
            return true;
        }
    }

    public void release(ServerErrorEvent event) {
        if (!properties.isDeduplicationEnabled()) {
            return;
        }

        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(key(event)), event.traceId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to release Discord alert deduplication key. traceId={}, failureType={}",
                    event.traceId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private String key(ServerErrorEvent event) {
        String source = String.join(
                "|",
                event.environment(),
                event.httpMethod(),
                normalizePath(event.requestUri()),
                event.exceptionType(),
                firstApplicationFrame(event)
        );
        return KEY_PREFIX + sha256(source);
    }

    private String normalizePath(String path) {
        String normalized = UUID_PATH_SEGMENT.matcher(path).replaceAll("/{id}");
        return NUMERIC_PATH_SEGMENT.matcher(normalized).replaceAll("/{id}");
    }

    private String firstApplicationFrame(ServerErrorEvent event) {
        StackTraceElement[] stackTrace = event.cause().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("com.valanse.valanse")) {
                return frame(element);
            }
        }
        return stackTrace.length == 0 ? "(no stack)" : frame(stackTrace[0]);
    }

    private String frame(StackTraceElement element) {
        return element.getClassName() + ":" + element.getMethodName() + ":" + element.getLineNumber();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
