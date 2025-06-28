// src/main/java/com/valanse/valanse/service/VoteService/VoteService.java
package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.dto.Vote.HotIssueVoteResponse;
import com.valanse.valanse.dto.Vote.VoteDetailResponse;
import com.valanse.valanse.dto.Vote.VoteResponseDto;

public interface VoteService {
    HotIssueVoteResponse getHotIssueVote();
    // 사용자의 투표를 처리하는 핵심 메서드.
    // userId: 현재 로그인한 사용자의 ID
    // voteId: 사용자가 투표하려는 투표의 ID
    // voteOptionId: 사용자가 선택한 투표 옵션의 ID
    VoteResponseDto processVote(Long userId, Long voteId, Long voteOptionId);

    VoteDetailResponse getVoteDetailById(Long voteId);
}