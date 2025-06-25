package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.dto.Comment.CommentRequest;

public interface CommentService {
    Long createComment(Long voteId, Long userId, CommentRequest request);
}
