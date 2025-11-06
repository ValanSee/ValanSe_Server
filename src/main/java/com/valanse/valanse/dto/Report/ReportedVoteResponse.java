package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.Vote;
import lombok.Getter;

import java.util.List;

@Getter
public class ReportedVoteResponse {

    private Long voteId;
    private String title;
    private String content; // 투표 상세 내용
    private String category;
    private int totalVoteCount;
    private String createdAt;

    public ReportedVoteResponse(Vote vote) {
        this.category = vote.getCategory().name();
        this.content = vote.getContent();
        this.createdAt = vote.getCreatedAt().toString();
        this.title = vote.getTitle();
        this.totalVoteCount = vote.getTotalVoteCount();
        this.voteId = vote.getId();
    }
}
