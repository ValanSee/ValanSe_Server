package com.valanse.valanse.service.ContentSeedService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 콘텐츠 시드 실행(주간 자동 또는 수동) 1회의 전체 결과.
// postOutcomes/interactionOutcomes는 봇 순서대로, 게시글 생성 전체가 끝난 뒤
// 상호작용 생성이 시작된다는 실행 순서를 그대로 반영한다.
// savedPostIds는 이번 실행에서 새로 저장된 게시글(Vote)의 ID 목록으로, Discord 알림에서
// 게시글 링크를 만드는 데 사용한다 - 상호작용은 기존 게시글을 대상으로 하므로 포함하지 않는다.
public record ContentSeedRunResult(
        List<ContentSeedBatchOutcome> postOutcomes,
        List<ContentSeedBatchOutcome> interactionOutcomes,
        ContentSeedUsageSummary usage,
        List<Long> savedPostIds
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

    // 품질 거절/저장 실패 사유별 발생 횟수. Discord 알림 등에서 원인 요약으로 사용한다.
    public Map<String, Long> rejectionReasonCounts() {
        return Stream.concat(postOutcomes.stream(), interactionOutcomes.stream())
                .flatMap(outcome -> outcome.failures().stream())
                .collect(Collectors.groupingBy(ContentSeedItemFailure::reason, Collectors.counting()));
    }
}
