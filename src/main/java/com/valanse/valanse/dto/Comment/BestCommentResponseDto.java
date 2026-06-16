package com.valanse.valanse.dto.Comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
/**
 * BestCommentResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record BestCommentResponseDto(

        @Schema(description = "댓글 전체 개수", example = "13")
        Integer totalCommentCount,

        @Schema(description = "가장 좋아요를 많이 받은 댓글 내용", example = "이거 완전 공감돼요!")
        String content
) {
}
