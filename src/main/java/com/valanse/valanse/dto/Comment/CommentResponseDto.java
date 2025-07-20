package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import com.querydsl.core.annotations.QueryProjection;


@Getter
@Builder
public class CommentResponseDto {
    private Long commentId;
    private Long voteId;
    private String nickname;
    private LocalDateTime createdAt;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime deletedAt;
    private String label;

    @QueryProjection
    public CommentResponseDto(Long commentId, Long voteId, String nickname, LocalDateTime createdAt,
                              String content, Integer likeCount, Integer replyCount,
                              LocalDateTime deletedAt, String label) {
        this.commentId = commentId;
        this.voteId = voteId;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.content = content;
        this.likeCount = likeCount;
        this.replyCount = replyCount;
        this.deletedAt = deletedAt;
        this.label = label;
    }
}

