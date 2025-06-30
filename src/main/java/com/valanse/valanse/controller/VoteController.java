// src/main/java/com/valanse/valanse/controller/VoteController.java
package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Vote.HotIssueVoteResponse;
import com.valanse.valanse.dto.Vote.VoteDetailResponse;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import com.valanse.valanse.service.VoteService.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3. 투표 API", description = "투표 생성, 조회, 참여 등 투표 관련 기능")
@RestController
@RequestMapping("/votes") // 투표 관련 API의 기본 경로
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(
            summary = "오늘의 핫이슈 밸런스 게임 선택지들 반환",
            description = "가장 많이 참여한 밸런스 게임 투표의 상세 정보와 옵션 목록을 조회합니다. " +
                    "총 참여자 수가 가장 많은 투표를 반환하며, 참여자 수가 동일한 경우 가장 최근에 생성된 투표를 우선합니다."
    )
    @GetMapping("/best") // 새로운 엔드포인트: /votes/best (GET 메서드)
    public ResponseEntity<HotIssueVoteResponse> getHotIssueVote() {
        HotIssueVoteResponse response = voteService.getHotIssueVote();
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "투표 선택, 취소, 재선택",
            description = "사용자가 투표 선택지를 클릭했을 때 호출됩니다. URL 경로에 있는 {voteId}와 {voteOptionId}를 통해 어떤 투표의 어떤 선택지를 조작하는지 명확히 전달합니다. 이미 투표한 선택지를 다시 클릭하면 투표가 취소되고, 다른 선택지를 클릭하면 기존 투표를 취소하고 새로운 선택지에 투표합니다."
    )
    @PostMapping("/{voteId}/vote-options/{voteOptionId}") // Path Variable 사용
    public ResponseEntity<VoteResponseDto> processVote(
            @PathVariable("voteId") Long voteId, // URL 경로에서 voteId를 추출
            @PathVariable("voteOptionId") Long voteOptionId) { // URL 경로에서 voteOptionId를 추출
        // 현재 로그인한 사용자의 ID를 SecurityContextHolder에서 가져옵니다.
        // Spring Security를 통해 인증된 사용자의 ID는 String 형태로 저장되어 있으므로 Long으로 변환합니다.
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        // 서비스 계층의 processVote 메서드를 호출하여 실제 비즈니스 로직을 수행합니다.
        VoteResponseDto response = voteService.processVote(userId, voteId, voteOptionId);

        // 서비스의 처리 결과를 HTTP 200 OK 상태 코드와 함께 클라이언트에게 반환합니다.
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "밸런스 게임 클릭했을 때 화면 불러오기",
            description = "ID를 통해 특정 투표의 상세 정보(제목, 옵션, 카테고리 등)를 조회합니다."
    )
    @GetMapping("/{voteId}")
    public ResponseEntity<VoteDetailResponse> getVoteDetail(@PathVariable("voteId") Long voteId) {
        VoteDetailResponse response = voteService.getVoteDetailById(voteId);
        return ResponseEntity.ok(response);
    }
}
