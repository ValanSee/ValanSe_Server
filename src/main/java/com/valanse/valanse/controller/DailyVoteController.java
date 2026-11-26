package com.valanse.valanse.controller;

import com.valanse.valanse.common.auth.SecurityUtils;
import com.valanse.valanse.dto.DailyVote.DailyVoteListResponse;
import com.valanse.valanse.dto.DailyVote.DailyVoteSummary;
import com.valanse.valanse.dto.DailyVote.DailyVoteTodayResponse;
import com.valanse.valanse.service.DailyVoteService.DailyVoteQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 오늘의 밸런스 공개 조회 API(오늘 게임, 지난 날 목록/상세)를 제공하는 컨트롤러 코드입니다. 비로그인도 조회할 수 있습니다.
 */
@RestController
@RequestMapping("/daily-votes")
@RequiredArgsConstructor
public class DailyVoteController {

    private final DailyVoteQueryService queryService;

    @Operation(
            summary = "오늘의 밸런스 조회",
            description = "KST 기준 오늘의 밸런스 게임(질문, 선택지, 오늘 표 수, 종료 시각, 어제 결과)을 반환합니다. " +
                    "로그인하면 내 참여 정보와 작성자 차단 여부가 함께 담깁니다. 오늘 게임이 없으면 204를 반환합니다."
    )
    @GetMapping("/today")
    public ResponseEntity<DailyVoteTodayResponse> getToday() {
        return queryService.getToday(SecurityUtils.currentMemberIdOrNull())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "지난 오늘의 밸런스 목록",
            description = "오늘 이전에 정산된 날만 날짜 내림차순으로 반환합니다. cursor(yyyy-MM-dd)는 해당 날짜 미만부터 조회하며, " +
                    "size 기본 20, 최대 50입니다. 다음 페이지가 없으면 nextCursor는 null입니다."
    )
    @GetMapping
    public DailyVoteListResponse getList(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return queryService.getList(cursor, size, SecurityUtils.currentMemberIdOrNull());
    }

    @Operation(
            summary = "특정 날짜의 오늘의 밸런스 조회",
            description = "yyyy-MM-dd 날짜의 오늘의 밸런스 요약을 반환합니다. 날짜 형식이 잘못되면 400, 없거나 미래 날짜면 404입니다."
    )
    @GetMapping("/{date}")
    public DailyVoteSummary getByDate(@PathVariable("date") String date) {
        return queryService.getByDate(date, SecurityUtils.currentMemberIdOrNull());
    }
}
