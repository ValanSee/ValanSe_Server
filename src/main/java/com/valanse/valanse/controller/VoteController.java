package com.valanse.valanse.controller;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import com.valanse.valanse.service.VoteService.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UserDetails;
import com.valanse.valanse.repository.MemberRepository;


import java.util.List;

@Tag(name = "3. Vote API", description = "투표 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/votes")
public class VoteController {

    private final VoteService voteService;
    private final MemberRepository memberRepository;  // 추가

    @GetMapping("/mine/created")
    public ResponseEntity<List<VoteResponseDto>> getMyCreatedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        List<VoteResponseDto> votes = voteService.getMyCreatedVotes(member, sort);
        return ResponseEntity.ok(votes);
    }

    @GetMapping("/mine/voted")
    public ResponseEntity<List<VoteResponseDto>> getMyVotedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        List<VoteResponseDto> votes = voteService.getMyVotedVotes(member, sort);
        return ResponseEntity.ok(votes);
    }
}
