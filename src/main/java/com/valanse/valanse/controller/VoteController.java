// src/main/java/com/valanse/valanse/controller/VoteController.java
package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Vote.HotIssueVoteResponse;
import com.valanse.valanse.service.VoteService.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. 투표 API", description = "투표 생성, 조회, 참여 등 투표 관련 기능")
@RestController
@RequestMapping("/votes") // 투표 관련 API의 기본 경로
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(
            summary = "오늘의 핫이슈 밸런스 게임 옵션 조회",
            description = "가장 많이 참여한 밸런스 게임 투표의 상세 정보와 옵션 목록을 조회합니다. " +
                    "총 참여자 수가 가장 많은 투표를 반환하며, 참여자 수가 동일한 경우 가장 최근에 생성된 투표를 우선합니다."
    )
    @GetMapping("/best") // 새로운 엔드포인트: /votes/best (GET 메서드)
    public ResponseEntity<HotIssueVoteResponse> getHotIssueVote() {
        HotIssueVoteResponse response = voteService.getHotIssueVote();
        return ResponseEntity.ok(response);
    }
}