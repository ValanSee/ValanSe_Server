package com.valanse.valanse.common.alert;

import java.time.Instant;

/**
 * HTTP 요청 처리 중 발생한 서버 오류를 후속 알림 계층에 전달하는 이벤트입니다.
 */
public record ServerErrorEvent(
        Instant occurredAt,
        String environment,
        int status,
        String httpMethod,
        String requestUri,
        String exceptionType,
        String exceptionMessage,
        String traceId,
        Throwable cause
) {
}
