package com.valanse.valanse.dto.DailyVote;

/**
 * 오늘의 밸런스 참여로 지급된 포인트와 당일 중복 지급 여부를 담는 DTO 코드입니다.
 */
public record DailyVoteReward(int point, boolean alreadyRewarded) {
}
