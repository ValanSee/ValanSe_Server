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
public class VotesCheckServiceImpl implements VotesCheckService {

    private final VoteResultQueryRepository voteResultQueryRepository;

    @Override
    public VoteGenderResultResponseDto getGenderVoteResult(Long voteId, String gender) {
        return voteResultQueryRepository.findVoteResultByGender(voteId, gender);
    }

    @Override
    public VoteAgeResultResponseDto getAgeVoteResult(Long voteId) {
        return voteResultQueryRepository.findVoteResultByAge(voteId);
    }

    @Override
    public VoteMbtiResultResponseDto getVoteResultByMbti(Long voteId, String mbtiType) {
        return voteResultQueryRepository.findVoteResultByMbti(voteId, mbtiType);
    }


}
