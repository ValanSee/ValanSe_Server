package com.valanse.valanse.dto.Vote;

import com.valanse.valanse.domain.Vote;
import lombok.Getter;

@Getter
public class VoteResponseDto {
    private Long voteId;
    private String title;
    private String category;
    private int totalVoteCount;
    private String createdAt;
    private boolean isDeleted;

    public VoteResponseDto(Vote vote) {
        this.voteId = vote.getId();
        this.title = vote.getTitle();
        this.category = vote.getCategory().name(); // enum to string
        this.totalVoteCount = vote.getTotalVoteCount();
        this.createdAt = vote.getCreatedAt().toString();
        this.isDeleted = vote.isDeleted();
    }
}
