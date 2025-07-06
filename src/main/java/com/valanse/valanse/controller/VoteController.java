package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Vote.VoteResponseDto;
import com.valanse.valanse.service.VoteService.VoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.valanse.valanse.domain.enums.VoteCategory;

import java.util.List;

@Tag(name = "투표 API", description = "투표 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/votes")
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/mine/created")
    public ResponseEntity<List<VoteResponseDto>> getMyCreatedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category,       // 추가
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        VoteCategory voteCategory = null;

        if (category != null && !category.isEmpty()) {
            voteCategory = convertCategory(category);
        }

        List<VoteResponseDto> votes = voteService.getMyCreatedVotes(memberId, sort, voteCategory);
        return ResponseEntity.ok(votes);
    }

    @GetMapping("/mine/voted")
    public ResponseEntity<List<VoteResponseDto>> getMyVotedVotes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String category,       // 추가
            @RequestParam(defaultValue = "latest") String sort
    ) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        VoteCategory voteCategory = null;

        if (category != null && !category.isEmpty()) {
            voteCategory = convertCategory(category);
        }

        List<VoteResponseDto> votes = voteService.getMyVotedVotes(memberId, sort, voteCategory);
        return ResponseEntity.ok(votes);
    }

    private VoteCategory convertCategory(String category) {
        try {
            return VoteCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
    }

}
