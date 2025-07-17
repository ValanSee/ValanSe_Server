package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.dto.Vote.*;
import org.springframework.data.domain.Pageable;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.dto.Vote.VoteResponseDto;

import java.util.List;

public interface VoteService {
    HotIssueVoteResponse getHotIssueVote();
    // 사용자의 투표를 처리하는 핵심 메서드.
    // userId: 현재 로그인한 사용자의 ID
    // voteId: 사용자가 투표하려는 투표의 ID
    // voteOptionId: 사용자가 선택한 투표 옵션의 ID
    VoteCancleResponseDto processVote(Long userId, Long voteId, Long voteOptionId);

    VoteDetailResponse getVoteDetailById(Long voteId);
    List<VoteResponseDto> getMyCreatedVotes(Long memberId, String sort, VoteCategory category);
    Long createVote(Long userId, VoteCreateRequest request);
    List<VoteResponseDto> getMyVotedVotes(Long memberId, String sort, VoteCategory category);
    // /votes get 메서드 cursor기반으로 변경
    VoteListResponse getVotesByCategoryAndSort(String category, String sort, String cursor, int size);
}