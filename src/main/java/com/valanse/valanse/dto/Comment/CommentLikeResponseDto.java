package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentLikeResponseDto {
    private Long commentId;
    private int likeCount;
    private String message;
}
