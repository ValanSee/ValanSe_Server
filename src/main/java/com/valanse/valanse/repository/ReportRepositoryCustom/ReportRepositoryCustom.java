package com.valanse.valanse.repository.ReportRepositoryCustom;

import com.querydsl.core.Tuple;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.ReportedTargetResponse;

import java.util.List;

public interface ReportRepositoryCustom {
    List<ReportedTargetResponse> findReportedTargets(ReportType type, String sort);
    long countReports(ReportType type, Long targetId);
}
