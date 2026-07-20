package com.valanse.valanse.dto.Vote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingVoteItemResponse {
    private int displayOrder;
    private TrendingDisplayType displayType;
    private Long voteId;
    private String title;
    private String content;
    private String category;
    private long reactivityScore;
    private long voteReactionCount;
    private long commentReactionCount;
    private Integer totalParticipants;
    private String createdBy;
    private String creatorTitle;
    private LocalDateTime createdAt;
    private List<HotIssueVoteOptionDto> options;
}
