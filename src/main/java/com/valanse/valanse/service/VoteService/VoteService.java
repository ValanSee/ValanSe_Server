package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.dto.Vote.VoteResponseDto;

import java.util.List;

public interface VoteService {

    List<VoteResponseDto> getMyCreatedVotes(Long memberId, String sort, VoteCategory category);

    List<VoteResponseDto> getMyVotedVotes(Long memberId, String sort, VoteCategory category);
}