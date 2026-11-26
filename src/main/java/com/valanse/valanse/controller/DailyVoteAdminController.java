package com.valanse.valanse.controller;

import com.valanse.valanse.dto.DailyVote.DailyVoteAdminItem;
import com.valanse.valanse.dto.DailyVote.DailyVoteReserveRequest;
import com.valanse.valanse.service.DailyVoteService.DailyVoteAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CMS 에서 오늘의 밸런스를 조회·예약·취소하는 관리자 전용 컨트롤러 코드입니다.
 */
@Tag(name = "관리자 오늘의 밸런스 API", description = "관리자 전용 오늘의 밸런스 조회/예약/취소 API")
@RestController
@RequestMapping("/admin/daily-votes")
@RequiredArgsConstructor
public class DailyVoteAdminController {

    private final DailyVoteAdminService adminService;

    @Operation(
            summary = "기간별 오늘의 밸런스 조회",
            description = "from~to(yyyy-MM-dd, 필수, 최대 62일) 범위에 행이 있는 날만 반환합니다. 빈 날짜는 응답에 없습니다."
    )
    @GetMapping
    public List<DailyVoteAdminItem> list(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to
    ) {
        return adminService.list(from, to);
    }

    @Operation(
            summary = "오늘의 밸런스 예약",
            description = "내일 이후 날짜에 선택지 2개인 게임을 예약합니다. 이미 행이 있으면 RESERVED 로 교체합니다."
    )
    @PutMapping("/{date}")
    public DailyVoteAdminItem reserve(@PathVariable("date") String date, @RequestBody DailyVoteReserveRequest request) {
        return adminService.reserve(date, request.getVoteId());
    }

    @Operation(
            summary = "오늘의 밸런스 예약 취소",
            description = "내일 이후 날짜의 행을 삭제합니다. 그날은 23:30에 다시 자동 선정됩니다."
    )
    @DeleteMapping("/{date}")
    public ResponseEntity<Void> cancel(@PathVariable("date") String date) {
        adminService.cancel(date);
        return ResponseEntity.noContent().build();
    }
}
