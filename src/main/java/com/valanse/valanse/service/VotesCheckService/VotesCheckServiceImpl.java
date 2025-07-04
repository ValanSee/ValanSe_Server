package com.valanse.valanse.service.VotesCheckService;

import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteResultQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VotesCheckServiceImpl implements VotesCheckService {

    private final VoteResultQueryRepository voteResultQueryRepository;

    @Override
    public VoteGenderResultResponseDto getGenderVoteResult(Long voteId, String gender) {
        return voteResultQueryRepository.findVoteResultByGender(voteId, gender);
    }
}
