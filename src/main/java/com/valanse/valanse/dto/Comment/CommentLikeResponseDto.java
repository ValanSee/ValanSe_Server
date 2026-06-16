package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
/**
 * CommentLikeResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class CommentLikeResponseDto {
    private Long commentId;
    private int likeCount;
    private String message;
}
