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
@Tag(name = "Vote API", description = "투표 관련 API") // Swagger UI 상단 카테고리
public class VoteController {

    private final VoteService voteService;

    @GetMapping("/mine")
    @Operation(
            summary = "내가 생성한 투표 조회",
            description = "type=created와 sort=latest 또는 oldest를 이용해 내가 생성한 투표를 조회합니다."
    )
    public ResponseEntity<List<VoteResponseDto>> getMyVotes(
            @AuthenticationPrincipal Member member,
            @RequestParam(defaultValue = "created") String type,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        if (!type.equals("created")) {
            throw new IllegalArgumentException("지원하지 않는 type입니다.");
        }

        List<VoteResponseDto> votes = voteService.getMyVotes(member, sort);
        return ResponseEntity.ok(votes);
    }
}
