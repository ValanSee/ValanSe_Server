package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Vote.VoteResponseDto;
import com.valanse.valanse.service.VoteService.VoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "3. Vote API", description = "투표 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/votes")
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/mine/created")
    public ResponseEntity<List<VoteResponseDto>> getMyCreatedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        List<VoteResponseDto> votes = voteService.getMyCreatedVotes(memberId, sort);
        return ResponseEntity.ok(votes);
    }

    @GetMapping("/mine/voted")
    public ResponseEntity<List<VoteResponseDto>> getMyVotedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        List<VoteResponseDto> votes = voteService.getMyVotedVotes(memberId, sort);
        return ResponseEntity.ok(votes);
    }
}
