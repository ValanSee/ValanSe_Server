package com.valanse.valanse.dto.Vote;

import java.time.LocalDateTime;

/**
 * Repository가 반환하는 투표별 반응성 집계 결과입니다.
 */
public record TrendingVoteScore(
        Long voteId,
        long voteReactionCount,
        long commentReactionCount,
        long reactivityScore,
        LocalDateTime latestReactionAt
) {

    public static TrendingVoteScore empty(Long voteId) {
        return new TrendingVoteScore(voteId, 0L, 0L, 0L, null);
    }
}
