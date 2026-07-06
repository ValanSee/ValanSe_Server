package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.enums.ReportType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportDetailResponse {
    private Long targetId;
    private ReportType targetType;
    private int reportCount;
    private ReportedVoteResponse vote;
    private ReportedCommentResponse comment;
    private List<ReportDetailItemResponse> reports;
}
