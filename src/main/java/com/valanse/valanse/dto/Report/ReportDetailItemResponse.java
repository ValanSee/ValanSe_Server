package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.enums.ReportReason;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportDetailItemResponse {
    private final Long reportId;
    private final Long reporterId;
    private final String reporterNickname;
    private final ReportReason reason;
    private final String reasonDescription;
    private final String content;
    private final LocalDateTime reportedAt;

    public ReportDetailItemResponse(Report report) {
        this.reportId = report.getId();
        this.reporterId = report.getMember().getId();
        this.reporterNickname = report.getMember().getNickname();
        this.reason = report.getReason();
        this.reasonDescription = report.getReason().description();
        this.content = report.getContent();
        this.reportedAt = report.getCreatedAt();
    }
}
