package com.valanse.valanse.dto.Comment;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
/**
 * 내 댓글 페이지 응답 값을 담는 DTO 코드입니다.
 */
public class PagedMyCommentResponse {
    private List<MyCommentResponseDto> comments;
    private int page;
    private int size;
    private boolean hasNext;
}
