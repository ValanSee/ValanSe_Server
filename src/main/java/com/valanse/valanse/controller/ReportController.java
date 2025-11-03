package com.valanse.valanse.controller;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Report.ReportRequest;
import com.valanse.valanse.service.ReportService.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신고 관련 API")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/{targetId}")
    public ResponseEntity<Void> Report(@AuthenticationPrincipal Member member, @PathVariable Long targetId, @RequestBody ReportRequest request) {
        reportService.report(member, targetId, request.getReportType());
        return ResponseEntity.ok().build();
    }

}
