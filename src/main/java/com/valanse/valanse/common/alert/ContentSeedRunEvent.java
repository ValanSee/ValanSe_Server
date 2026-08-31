package com.valanse.valanse.common.alert;

import com.valanse.valanse.service.ContentSeedService.ContentSeedRunResult;

import java.time.Instant;

// 콘텐츠 시드 실행(주간 자동 스케줄 또는 관리자 수동 실행) 1회의 결과를 Discord 알림 계층에
// 전달하는 이벤트. result가 null이면 오케스트레이션 자체가 예외로 중단된 치명적 오류를 뜻하며,
// 이 경우 fatalErrorType/fatalErrorMessage에 원인 정보가 담긴다.
public record ContentSeedRunEvent(
        Instant occurredAt,
        String environment,
        String trigger,
        long durationMs,
        ContentSeedRunResult result,
        String frontendBaseUrl,
        String fatalErrorType,
        String fatalErrorMessage
) {
    public static ContentSeedRunEvent completed(
            String environment,
            String trigger,
            Instant occurredAt,
            long durationMs,
            ContentSeedRunResult result,
            String frontendBaseUrl
    ) {
        return new ContentSeedRunEvent(
                occurredAt, environment, trigger, durationMs, result, frontendBaseUrl, null, null);
    }

    public static ContentSeedRunEvent fatal(
            String environment,
            String trigger,
            Instant occurredAt,
            long durationMs,
            Throwable cause
    ) {
        return new ContentSeedRunEvent(
                occurredAt, environment, trigger, durationMs, null, null,
                cause.getClass().getSimpleName(),
                cause.getMessage() != null ? cause.getMessage() : "(no message)");
    }

    public boolean isFatal() {
        return result == null;
    }
}
