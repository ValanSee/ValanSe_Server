package com.valanse.valanse.common.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DiscordAlertDeduplicatorTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private DiscordAlertProperties properties;
    private DiscordAlertDeduplicator deduplicator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        properties = new DiscordAlertProperties();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        deduplicator = new DiscordAlertDeduplicator(redisTemplate, properties);
    }

    @Test
    @DisplayName("최초 오류는 TTL과 함께 Redis에 등록하고 전송을 허용한다")
    void shouldSend_FirstOccurrence_ReturnsTrue() {
        ServerErrorEvent event = event("/votes/123", "trace-1");
        when(valueOperations.setIfAbsent(anyString(), eq("trace-1"), eq(Duration.ofMinutes(1))))
                .thenReturn(true);

        assertThat(deduplicator.shouldSend(event)).isTrue();

        verify(valueOperations).setIfAbsent(
                anyString(),
                eq("trace-1"),
                eq(Duration.ofMinutes(1))
        );
    }

    @Test
    @DisplayName("Redis에 같은 fingerprint가 있으면 중복 알림을 차단한다")
    void shouldSend_Duplicate_ReturnsFalse() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(1))))
                .thenReturn(false);

        assertThat(deduplicator.shouldSend(event("/votes/123", "trace-2"))).isFalse();
    }

    @Test
    @DisplayName("Redis 장애 시 알림을 허용한다")
    void shouldSend_WhenRedisFails_FailsOpen() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(1))))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        assertThat(deduplicator.shouldSend(event("/votes/123", "trace-3"))).isTrue();
    }

    @Test
    @DisplayName("중복 억제가 비활성화되면 Redis 조회 없이 알림을 허용한다")
    void shouldSend_WhenDisabled_DoesNotUseRedis() {
        properties.setDeduplicationEnabled(false);

        assertThat(deduplicator.shouldSend(event("/votes/123", "trace-4"))).isTrue();

        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("URI의 숫자 ID가 달라도 같은 오류 fingerprint를 사용한다")
    void shouldSend_DifferentNumericIds_UsesSameFingerprint() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(1))))
                .thenReturn(true);

        deduplicator.shouldSend(event("/votes/123/comments/456", "trace-1"));
        deduplicator.shouldSend(event("/votes/999/comments/888", "trace-2"));

        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations, org.mockito.Mockito.times(2)).setIfAbsent(
                keyCaptor.capture(),
                anyString(),
                eq(Duration.ofMinutes(1))
        );
        assertThat(keyCaptor.getAllValues()).hasSize(2);
        assertThat(keyCaptor.getAllValues().get(0)).isEqualTo(keyCaptor.getAllValues().get(1));
    }

    private ServerErrorEvent event(String uri, String traceId) {
        RuntimeException cause = new RuntimeException("boom");
        cause.setStackTrace(new StackTraceElement[]{
                new StackTraceElement(
                        "com.valanse.valanse.service.VoteService.VoteServiceImpl",
                        "findVote",
                        "VoteServiceImpl.java",
                        123
                )
        });
        return new ServerErrorEvent(
                Instant.parse("2026-07-24T00:00:00Z"),
                "dev-server",
                500,
                "GET",
                uri,
                "RuntimeException",
                "boom",
                traceId,
                cause
        );
    }
}
