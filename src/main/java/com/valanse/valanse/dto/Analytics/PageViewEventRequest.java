package com.valanse.valanse.dto.Analytics;

/**
 * PageViewEventRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public record PageViewEventRequest(
        String anonymousId,
        String pagePath
) {
}
