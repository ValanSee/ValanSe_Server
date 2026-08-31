package com.valanse.valanse.service.ContentSeedService;

// Claude 호출 1회의 결과. inputTokens/outputTokens는 사용량·비용 집계에 사용한다.
public record GenerationResult<T>(
        T content,
        long inputTokens,
        long outputTokens
) {
}
