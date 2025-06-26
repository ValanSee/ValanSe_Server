package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;

    public List<VoteResponseDto> getMyCreatedVotes(Member member, String sort) {
        List<Vote> votes = sort.equals("latest") ?
                voteRepository.findAllByMemberOrderByCreatedAtDesc(member) :
                voteRepository.findAllByMemberOrderByCreatedAtAsc(member);

        return votes.stream().map(VoteResponseDto::new).collect(Collectors.toList());
    }

    public List<VoteResponseDto> getMyVotedVotes(Member member, String sort) {
        List<Vote> votes = sort.equals("latest") ?
                voteRepository.findAllByMemberVotedOrderByCreatedAtDesc(member) :
                voteRepository.findAllByMemberVotedOrderByCreatedAtAsc(member);

        return votes.stream().map(VoteResponseDto::new).collect(Collectors.toList());
    }
}
