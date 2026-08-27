package com.valanse.valanse.service.ContentSeedService;

import java.math.BigDecimal;

// 실행 1회(주간 자동 또는 수동) 동안의 Claude API 사용량 집계.
// apiCallCount는 부족분 재시도 호출까지 포함한 실제 Claude 호출 횟수이고,
// estimatedCostUsd는 ContentSeedProperties.Pricing 단가 기준의 예상 비용이다.
public record ContentSeedUsageSummary(
        int apiCallCount,
        long inputTokens,
        long outputTokens,
        BigDecimal estimatedCostUsd
) {
    public static ContentSeedUsageSummary empty() {
        return new ContentSeedUsageSummary(0, 0, 0, BigDecimal.ZERO);
    }
}
