package com.valanse.valanse.service.CommentLikeService;

import com.valanse.valanse.dto.Comment.CommentLikeResponseDto;

/**
 * CommentLikeService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface CommentLikeService {
    CommentLikeResponseDto likeComment(Long voteId, Long commentId);
}
