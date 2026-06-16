package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
/**
 * PagedCommentResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class PagedCommentResponse {
    private List<CommentResponseDto> comments;
    private int page;
    private int size;
    private boolean hasNext;
}