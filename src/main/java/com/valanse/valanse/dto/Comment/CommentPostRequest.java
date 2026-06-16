package com.valanse.valanse.dto.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * CommentPostRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public class CommentPostRequest {
    private String content;
    private Long parentId; // null이면 최상위 댓글
}
