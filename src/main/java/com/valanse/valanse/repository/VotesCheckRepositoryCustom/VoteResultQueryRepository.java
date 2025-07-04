package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;

public interface VoteResultQueryRepository {
    VoteGenderResultResponseDto findVoteResultByGender(Long voteId, String gender);
}
