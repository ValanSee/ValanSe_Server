package com.valanse.valanse.dto.Comment;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequest {
    private String content;
    private Long parentId; // null이면 최상위 댓글
}
