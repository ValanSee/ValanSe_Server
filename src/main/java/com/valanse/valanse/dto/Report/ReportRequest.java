package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.enums.ReportReason;
import com.valanse.valanse.domain.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
/**
 * ReportRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public class ReportRequest {
    @NotNull(message = "신고 대상 유형을 입력해주세요.")
    private ReportType reportType;

    @NotNull(message = "신고 사유를 입력해주세요.")
    private ReportReason reason;

    @Size(max = 1000, message = "신고 상세 내용은 1,000자 이하로 입력해주세요.")
    private String content;
}
