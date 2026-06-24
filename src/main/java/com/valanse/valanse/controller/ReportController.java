package com.valanse.valanse.controller;

import com.valanse.valanse.common.auth.SecurityUtils;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.domain.enums.Role;
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
/**
 * 투표와 댓글 신고 생성 및 관리자 신고 목록 조회를 처리하는 컨트롤러 코드입니다.
 */
public class ReportController {

    private final ReportService reportService;
    private final MemberService memberService;

    /**
     * ReportController의 Report 기능을 수행하는 메서드입니다.
     */
    @PostMapping("/{targetId}")
    public ResponseEntity<Void> Report(@PathVariable Long targetId, @RequestBody ReportRequest request) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);

        reportService.report(member, targetId, request.getReportType(), request.getReason(), request.getContent());
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자가 신고 누적 대상 목록을 조회하는 메서드입니다.
     */
    @GetMapping
    public ResponseEntity<List<ReportedTargetResponse>> getReportedTargets(
            @RequestParam ReportType type,
            @RequestParam(defaultValue = "latest")  String sort) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Member member = SecurityUtils.isCurrentUserAdmin()
                ? Member.builder().id(loginId).role(Role.ADMIN).build()
                : memberService.findById(loginId);

        List<ReportedTargetResponse> results = reportService.getReportedTargets(member, type, sort);
        return ResponseEntity.ok().body(results);
    }

}
