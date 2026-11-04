package com.valanse.valanse.dto.Comment;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자(CMS)가 AI 생성 콘텐츠를 검수할 때 쓰는 댓글/대댓글 통합 조회 응답 DTO입니다.
 * 부모 댓글과 대댓글을 하나의 목록으로 다루므로 isReply/parentCommentId를 함께 내려준다.
 */
@Getter
public class AdminCommentResponseDto {

    private final Long commentId;
    private final String content;
    private final String nickname;
    private final Boolean isBot;
    private final LocalDateTime createdAt;
    private final Boolean isReply;
    private final Long parentCommentId;
    private final Long voteId;
    private final String voteTitle;

    @QueryProjection
    public AdminCommentResponseDto(
            Long commentId,
            String content,
            String nickname,
            Boolean isBot,
            LocalDateTime createdAt,
            Boolean isReply,
            Long parentCommentId,
            Long voteId,
            String voteTitle
    ) {
        this.commentId = commentId;
        this.content = content;
        this.nickname = nickname != null ? nickname : "탈퇴한 사용자";
        this.isBot = isBot != null && isBot;
        this.createdAt = createdAt;
        this.isReply = Boolean.TRUE.equals(isReply);
        this.parentCommentId = parentCommentId;
        this.voteId = voteId;
        this.voteTitle = voteTitle;
    }
}
