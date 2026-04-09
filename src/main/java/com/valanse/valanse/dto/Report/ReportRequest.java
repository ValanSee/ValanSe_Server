package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.enums.ReportType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportRequest {
    private ReportType reportType;
}
