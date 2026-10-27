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
    // 토글 이후 현재 요청자의 좋아요 상태 (true: 좋아요 됨, false: 취소됨)
    private boolean liked;
    private String message;
}
