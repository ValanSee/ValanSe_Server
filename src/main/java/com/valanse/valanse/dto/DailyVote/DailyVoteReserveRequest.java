package com.valanse.valanse.dto.DailyVote;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특정 날짜에 예약할 게임 id 를 담는 관리자 요청 DTO 코드입니다.
 */
@Getter
@NoArgsConstructor
public class DailyVoteReserveRequest {
    private Long voteId;

    public DailyVoteReserveRequest(Long voteId) {
        this.voteId = voteId;
    }
}
