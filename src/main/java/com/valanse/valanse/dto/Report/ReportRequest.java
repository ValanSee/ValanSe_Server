package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.enums.ReportType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
/**
 * ReportRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public class ReportRequest {
    private ReportType reportType;
}
