package com.valanse.valanse.repository.ReportRepositoryCustom;

import com.querydsl.core.Tuple;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;

import java.util.List;

/**
 * DB 조회와 저장을 담당하는 레포지토리 코드입니다.
 */
public interface ReportRepositoryCustom {
    List<ReportedTargetResponse> findReportedTargets(ReportType type, String sort);
}
