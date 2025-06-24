// valansee/valanse_server/ValanSe_Server-bf9a02cd3dab4e835ce04f412ddb998d88bceaf7/src/main/java/com/valanse/valanse/controller/VoteController.java
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
@RequestMapping("/votes") // 투표 관련 API의 기본 경로를 /votes로 변경
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(
            summary = "오늘의 핫이슈 밸런스 게임 옵션 조회",
            description = "가장 많이 참여한 밸런스 게임 투표의 상세 정보와 옵션 목록을 조회합니다."
    )
    @GetMapping("/best") // 엔드포인트를 /best로 설정
    public ResponseEntity<HotIssueVoteResponse> getHotIssueVote() {
        HotIssueVoteResponse response = voteService.getHotIssueVote();
        return ResponseEntity.ok(response);
    }
}