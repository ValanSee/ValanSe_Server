package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import com.querydsl.core.annotations.QueryProjection;


@Getter
@Builder
public class CommentResponseDto {
    private Long voteId;
    private String nickname;
    private LocalDateTime createdAt;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isDeleted;
    private String label;

    @QueryProjection
    public CommentResponseDto(Long voteId, String nickname, LocalDateTime createdAt,
                              String content, Integer likeCount, Integer replyCount,
                              Boolean isDeleted, String label) {
        this.voteId = voteId;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.content = content;
        this.likeCount = likeCount;
        this.replyCount = replyCount;
        this.isDeleted = isDeleted;
        this.label = label;
    }
}

