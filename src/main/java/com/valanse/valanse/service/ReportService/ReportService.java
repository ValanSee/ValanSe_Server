package com.valanse.valanse.service.ReportService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.ReportType;

public interface ReportService {

    void report(Member member, Long targetId, ReportType reportType);
}
