package com.valanse.valanse.controller;

import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;
import com.valanse.valanse.service.VotesCheckService.VotesCheckService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "투표 결과 조회 API", description = "사용자의 특징 별 투표 결과 확인 기능")
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

    @GetMapping("/{voteId}/age")
    public ResponseEntity<VoteAgeResultResponseDto> getVoteAgeResult(@PathVariable("voteId") Long voteId) {
        VoteAgeResultResponseDto result = votesCheckService.getAgeVoteResult(voteId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/votes/{voteId}/mbti")
    public ResponseEntity<VoteMbtiResultResponseDto> getVoteResultByMbti(
            @PathVariable Long voteId,
            @RequestParam("mbti_type") String mbtiType) {
        VoteMbtiResultResponseDto result = votesCheckService.getVoteResultByMbti(voteId, mbtiType);
        return ResponseEntity.ok(result);
    }


}
