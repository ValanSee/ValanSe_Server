package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Analytics.PageViewEventRequest;
import com.valanse.valanse.dto.Analytics.PageViewEventResponse;
import com.valanse.valanse.service.AnalyticsService.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "분석 이벤트 API", description = "페이지 방문 등 사용자 활동 이벤트 수집 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/analytics/events")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/page-view")
    @Operation(summary = "페이지 방문 이벤트 수집", description = "비로그인/로그인 사용자의 페이지 방문 이벤트를 기록합니다.")
    public ResponseEntity<PageViewEventResponse> recordPageView(
            @RequestBody PageViewEventRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(analyticsService.recordPageView(request, authentication));
    }
}
