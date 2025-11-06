package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReportedCommentResponse {

    private Long commentId;
    private Long voteId;
    private String nickname;
    private String content;
    private Integer likeCount;
    private Integer replyCount;

    public ReportedCommentResponse(Comment comment) {
        this.commentId = comment.getId();
        this.content = comment.getContent();
        this.likeCount = comment.getLikeCount();
        this.nickname = comment.getMember().getNickname();
        this.replyCount = comment.getReplyCount();
        this.voteId = comment.getCommentGroup().getVote().getId();
    }
}
