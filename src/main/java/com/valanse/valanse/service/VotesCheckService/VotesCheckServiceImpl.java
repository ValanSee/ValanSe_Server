package com.valanse.valanse.service.VotesCheckService;

import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteResultQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 투표 결과 통계를 조건별로 조회하는 서비스 코드입니다.
 */
public class VotesCheckServiceImpl implements VotesCheckService {

    private final VoteResultQueryRepository voteResultQueryRepository;

    @Override
    /**
     * GenderVoteResult 정보를 조회하는 메서드입니다.
     */
    public VoteGenderResultResponseDto getGenderVoteResult(Long voteId, String gender) {
        return voteResultQueryRepository.findVoteResultByGender(voteId, gender);
    }

    @Override
    /**
     * AgeVoteResult 정보를 조회하는 메서드입니다.
     */
    public VoteAgeResultResponseDto getAgeVoteResult(Long voteId) {
        return voteResultQueryRepository.findVoteResultByAge(voteId);
    }

    @Override
    /**
     * VoteResultByMbti 정보를 조회하는 메서드입니다.
     */
    public VoteMbtiResultResponseDto getVoteResultByMbti(Long voteId, String mbtiType) {
        return voteResultQueryRepository.findVoteResultByMbti(voteId, mbtiType);
    }


}
