package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;

import java.util.List;

public interface ReportService {

    // 신고 기능
    void report(Member member, Long targetId, ReportType reportType);
    // 신고 대상 조회
    List<ReportedTargetResponse> getReportedTargets(Member member,ReportType type, String sort);
}
