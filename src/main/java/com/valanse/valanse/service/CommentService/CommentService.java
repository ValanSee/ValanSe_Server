package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.dto.Comment.CommentPostRequest;

public interface CommentService {
    Long createComment(Long voteId, Long userId, CommentPostRequest request);
}
