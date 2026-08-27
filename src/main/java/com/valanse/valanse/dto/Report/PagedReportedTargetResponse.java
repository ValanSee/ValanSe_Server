package com.valanse.valanse.dto.Report;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
/**
 * 신고 대상 목록 페이지 응답 값을 담는 DTO 코드입니다.
 */
public class PagedReportedTargetResponse {
    private List<ReportedTargetResponse> reports;
    private int page;
    private int size;
    private boolean hasNext;
}
