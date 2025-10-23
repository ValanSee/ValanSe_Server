package com.valanse.valanse.dto.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentPostRequest {
    private String content;
    private Long parentId; // null이면 최상위 댓글
}
