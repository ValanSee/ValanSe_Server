package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;

public interface VoteResultQueryRepository {
    VoteGenderResultResponseDto findVoteResultByGender(Long voteId, String gender);

    VoteAgeResultResponseDto findVoteResultByAge(Long voteId);

    VoteMbtiResultResponseDto findVoteResultByMbti(Long voteId, String mbtiType);


}
