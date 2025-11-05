package com.valanse.valanse.controller;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportRequest;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.ReportService.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "신고 관련 API")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final MemberService memberService;

    @PostMapping("/{targetId}")
    public ResponseEntity<Void> Report(@PathVariable Long targetId, @RequestBody ReportRequest request) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);

        reportService.report(member, targetId, request.getReportType());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ReportedTargetResponse>> getReportedTargets(
            @RequestParam ReportType type,
            @RequestParam(defaultValue = "latest")  String sort) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);

        List<ReportedTargetResponse> results = reportService.getReportedTargets(member, type, sort);
        return ResponseEntity.ok().body(results);
    }

}
