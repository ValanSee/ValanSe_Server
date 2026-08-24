package com.valanse.valanse.service.BotVoteService;

/**
 * 봇 계정의 투표 참여를 처리하는 서비스입니다.
 */
public interface BotVoteService {

    /**
     * 봇 회원의 투표 관계만 저장합니다. Vote/VoteOption의 집계 카운트와 포인트 지급에는 영향을 주지 않습니다.
     */
    void castBotVote(Long botMemberId, Long voteOptionId);
}
