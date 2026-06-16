package com.valanse.valanse.service.VotesCheckService;

import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;

/**
 * VotesCheckService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface VotesCheckService {
    VoteGenderResultResponseDto getGenderVoteResult(Long voteId, String gender);

    VoteAgeResultResponseDto getAgeVoteResult(Long voteId);

    VoteMbtiResultResponseDto getVoteResultByMbti(Long voteId, String mbtiType);
}
