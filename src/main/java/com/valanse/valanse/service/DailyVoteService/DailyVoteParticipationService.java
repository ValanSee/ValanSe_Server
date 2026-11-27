package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.DailyVote.DailyVoteReward;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;

import java.util.Optional;

/**
 * 오늘의 밸런스 참여 기록, 포인트 지급, 오늘 표 집계의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface DailyVoteParticipationService {
    Optional<DailyVote> findTodayFor(Long voteId);

    boolean hasParticipated(DailyVote today, Long memberId);

    DailyVoteReward participate(DailyVote today, Member member, Long chosenOptionId);

    void updateCurrentOption(DailyVote today, Long memberId, Long optionIdOrNull);

    DailyVoteTally tally(DailyVote dv);
}
