package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.Vote;
import lombok.Getter;

import java.util.List;

@Getter
/**
 * ReportedVoteResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class ReportedVoteResponse {

    private Long voteId;
    private String title;
    private String content; // 투표 상세 내용
    private String category;
    private int totalVoteCount;
    private String createdAt;

    /**
     * ReportedVoteResponse 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    public ReportedVoteResponse(Vote vote) {
        this.category = vote.getCategory().name();
        this.content = vote.getContent();
        this.createdAt = vote.getCreatedAt().toString();
        this.title = vote.getTitle();
        this.totalVoteCount = vote.getTotalVoteCount();
        this.voteId = vote.getId();
    }
}
