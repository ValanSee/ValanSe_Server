package com.valanse.valanse.service.ContentSeedService;

import java.util.List;

// 봇 1명의 게시글 생성 또는 상호작용 생성 배치 1회 결과.
// targetCount는 목표 생성량(예: postsPerBot), savedCount는 실제로 저장에 성공한 수다.
public record ContentSeedBatchOutcome(
        Long botMemberId,
        int targetCount,
        int savedCount,
        List<ContentSeedItemFailure> failures
) {
    public int shortfall() {
        return Math.max(0, targetCount - savedCount);
    }
}
