package com.valanse.valanse.service.ContentSeedService;

import java.util.List;

// 콘텐츠 시드 실행(주간 자동 또는 수동) 1회의 전체 결과.
// postOutcomes/interactionOutcomes는 봇 순서대로, 게시글 생성 전체가 끝난 뒤
// 상호작용 생성이 시작된다는 실행 순서를 그대로 반영한다.
public record ContentSeedRunResult(
        List<ContentSeedBatchOutcome> postOutcomes,
        List<ContentSeedBatchOutcome> interactionOutcomes
) {
    public int targetPostCount() {
        return postOutcomes.stream().mapToInt(ContentSeedBatchOutcome::targetCount).sum();
    }

    public int savedPostCount() {
        return postOutcomes.stream().mapToInt(ContentSeedBatchOutcome::savedCount).sum();
    }

    public int targetInteractionCount() {
        return interactionOutcomes.stream().mapToInt(ContentSeedBatchOutcome::targetCount).sum();
    }

    public int savedInteractionCount() {
        return interactionOutcomes.stream().mapToInt(ContentSeedBatchOutcome::savedCount).sum();
    }
}
