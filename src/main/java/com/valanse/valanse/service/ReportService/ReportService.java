package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ReportReason;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.PagedReportedTargetResponse;
import com.valanse.valanse.dto.Report.ReportDetailResponse;
import org.springframework.data.domain.Pageable;

/**
 * ReportService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface ReportService {

    // 신고 기능
    void report(Member member, Long targetId, ReportType reportType);
    void report(Member member, Long targetId, ReportType reportType, ReportReason reason, String content);
    // 신고 대상 조회
    PagedReportedTargetResponse getReportedTargets(Member member, ReportType type, String sort, Pageable pageable);
    ReportDetailResponse getReportDetail(Member member, ReportType type, Long targetId);
}
