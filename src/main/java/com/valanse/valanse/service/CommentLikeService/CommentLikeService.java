package com.valanse.valanse.service.CommentLikeService;

import com.valanse.valanse.dto.Comment.CommentLikeResponseDto;

public interface CommentLikeService {
    CommentLikeResponseDto likeComment(Long voteId, Long commentId);
}
