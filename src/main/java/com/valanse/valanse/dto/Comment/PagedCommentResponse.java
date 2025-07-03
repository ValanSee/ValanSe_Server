package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PagedCommentResponse {
    private List<CommentResponseDto> comments;
    private int page;
    private int size;
    private boolean hasNext;
}