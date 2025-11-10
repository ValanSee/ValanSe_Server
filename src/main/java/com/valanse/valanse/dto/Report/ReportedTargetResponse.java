package com.valanse.valanse.dto.Report;

import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportedTargetResponse {
    private Long targetId;
    private Long reportCount;
    private String targetType;
    private ReportedVoteResponse vote;
    private ReportedCommentResponse comment;
}
