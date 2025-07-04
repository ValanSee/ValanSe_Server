package com.valanse.valanse.controller;

import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.service.VotesCheckService.VotesCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/votes")
public class VotesCheckController {

    private final VotesCheckService votesCheckService;

    @GetMapping("/{voteId}/gender/female")
    public ResponseEntity<VoteGenderResultResponseDto> getFemaleVoteResult(@PathVariable("voteId") Long voteId) {
        VoteGenderResultResponseDto result = votesCheckService.getGenderVoteResult(voteId, "F");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{voteId}/gender/male")
    public ResponseEntity<VoteGenderResultResponseDto> getMaleVoteResult(@PathVariable("voteId") Long voteId) {
        VoteGenderResultResponseDto result = votesCheckService.getGenderVoteResult(voteId, "M");
        return ResponseEntity.ok(result);
    }
}
