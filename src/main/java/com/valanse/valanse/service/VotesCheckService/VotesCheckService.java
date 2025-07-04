package com.valanse.valanse.service.VotesCheckService;

import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;

public interface VotesCheckService {
    VoteGenderResultResponseDto getGenderVoteResult(Long voteId, String gender);
}
