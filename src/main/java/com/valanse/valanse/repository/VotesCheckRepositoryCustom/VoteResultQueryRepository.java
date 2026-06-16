package com.valanse.valanse.repository.VotesCheckRepositoryCustom;

import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;

/**
 * VoteResultQueryRepository 엔티티의 DB 접근을 담당하는 레포지토리 코드입니다.
 */
public interface VoteResultQueryRepository {
    VoteGenderResultResponseDto findVoteResultByGender(Long voteId, String gender);

    VoteAgeResultResponseDto findVoteResultByAge(Long voteId);

    VoteMbtiResultResponseDto findVoteResultByMbti(Long voteId, String mbtiType);


}
