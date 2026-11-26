package com.valanse.valanse.dto.DailyVote;

/**
 * 오늘의 밸런스 투표 응답에 담는 이번 요청의 참여 보상과 오늘 표 집계 DTO 코드입니다.
 */
public record DailyVoteStatus(DailyVoteReward reward, DailyVoteTally todayTally) {
}
