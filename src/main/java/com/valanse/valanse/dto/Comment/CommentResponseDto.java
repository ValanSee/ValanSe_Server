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
    private LocalDateTime commentCreatedAt;  // 추가
    private LocalDateTime voteCreatedAt;     // 추가
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime deletedAt;
    private String voteOptionLabel;
    private Long daysAgo;
    private Long hoursAgo;

    @QueryProjection
    public CommentResponseDto(Long commentId, Long voteId, String nickname,
                              LocalDateTime commentCreatedAt, LocalDateTime voteCreatedAt,  // 추가
                              String content, Integer likeCount, Integer replyCount,
                              LocalDateTime deletedAt, String voteOptionLabel, Long daysAgo, Long hoursAgo) {
        this.commentId = commentId;
        this.voteId = voteId;
        this.nickname = nickname;
        this.commentCreatedAt = commentCreatedAt;
        this.voteCreatedAt = voteCreatedAt;
        this.content = content;
        this.likeCount = likeCount;
        this.replyCount = replyCount;
        this.deletedAt = deletedAt;
        this.voteOptionLabel = voteOptionLabel;
        this.daysAgo = daysAgo;
        this.hoursAgo = hoursAgo;
    }
}
