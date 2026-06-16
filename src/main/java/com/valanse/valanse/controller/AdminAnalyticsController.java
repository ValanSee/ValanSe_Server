package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Analytics.MauResponse;
import com.valanse.valanse.service.AnalyticsService.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 분석 API", description = "관리자 전용 사용자 분석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/analytics")
/**
 * 관리자용 MAU 분석 조회 요청을 처리하는 컨트롤러 코드입니다.
 */
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Mau 정보를 조회하는 메서드입니다.
     */
    @GetMapping("/mau")
    @Operation(summary = "MAU 조회", description = "한국 시간 기준 특정 월의 비로그인/로그인/탈퇴/전체 MAU를 조회합니다.")
    public ResponseEntity<MauResponse> getMau(@RequestParam(required = false) String yearMonth) {
        return ResponseEntity.ok(analyticsService.getMonthlyActiveUsers(yearMonth));
    }
}
