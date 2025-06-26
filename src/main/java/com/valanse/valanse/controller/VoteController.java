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

import java.util.List;

@Tag(name = "3. Vote API", description = "투표 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/votes")
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/mine/created")
    @Operation(summary = "내가 생성한 투표 조회", description = "내가 만든 밸런스 게임을 최신순 또는 오래된 순으로 조회합니다.")
    public ResponseEntity<List<VoteResponseDto>> getMyCreatedVotes(
            @AuthenticationPrincipal Member member,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        List<VoteResponseDto> votes = voteService.getMyCreatedVotes(member, sort);
        return ResponseEntity.ok(votes);
    }

    @GetMapping("/mine/voted")
    @Operation(summary = "내가 투표한 밸런스 게임 조회", description = "내가 참여한 밸런스 게임을 최신순 또는 오래된 순으로 조회합니다.")
    public ResponseEntity<List<VoteResponseDto>> getMyVotedVotes(
            @AuthenticationPrincipal Member member,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        List<VoteResponseDto> votes = voteService.getMyVotedVotes(member, sort);
        return ResponseEntity.ok(votes);
    }
}
