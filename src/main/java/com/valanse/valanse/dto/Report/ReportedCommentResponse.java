package com.valanse.valanse.dto.Report;

import com.valanse.valanse.domain.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
/**
 * ReportedCommentResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class ReportedCommentResponse {

    private Long commentId;
    private Long voteId;
    private String nickname;
    private String content;
    private Integer likeCount;
    private Integer replyCount;

    /**
     * ReportedCommentResponse 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    public ReportedCommentResponse(Comment comment) {
        this.commentId = comment.getId();
        this.content = comment.getContent();
        this.likeCount = comment.getLikeCount();
        this.nickname = comment.getMember().getNickname();
        this.replyCount = comment.getReplyCount();
        this.voteId = comment.getCommentGroup().getVote().getId();
    }
}
