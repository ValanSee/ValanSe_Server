package com.valanse.valanse.service.VotesCheckService;

import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;

public interface VotesCheckService {
    VoteGenderResultResponseDto getGenderVoteResult(Long voteId, String gender);

    VoteAgeResultResponseDto getAgeVoteResult(Long voteId);

    VoteMbtiResultResponseDto getVoteResultByMbti(Long voteId, String mbtiType);
}
