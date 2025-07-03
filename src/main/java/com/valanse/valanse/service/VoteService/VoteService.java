package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final MemberRepository memberRepository;

    public List<VoteResponseDto> getMyCreatedVotes(Long memberId, String sort) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException("회원이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        List<Vote> votes = sort.equals("latest") ?
                voteRepository.findAllByMemberOrderByCreatedAtDesc(member) :
                voteRepository.findAllByMemberOrderByCreatedAtAsc(member);

        return votes.stream().map(VoteResponseDto::new).collect(Collectors.toList());
    }

    public List<VoteResponseDto> getMyVotedVotes(Long memberId, String sort) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ApiException("회원이 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        List<Vote> votes = sort.equals("latest") ?
                voteRepository.findAllByMemberVotedOrderByCreatedAtDesc(member) :
                voteRepository.findAllByMemberVotedOrderByCreatedAtAsc(member);

        return votes.stream().map(VoteResponseDto::new).collect(Collectors.toList());
    }
}
