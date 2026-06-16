package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;

import java.util.List;

/**
 * ReportService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface ReportService {

    // 신고 기능
    void report(Member member, Long targetId, ReportType reportType);
    // 신고 대상 조회
    List<ReportedTargetResponse> getReportedTargets(Member member,ReportType type, String sort);
}
