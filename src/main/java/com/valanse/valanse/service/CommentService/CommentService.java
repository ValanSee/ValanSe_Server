package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.dto.Comment.BestCommentResponseDto;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Comment.PagedCommentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    Long createComment(Long voteId, Long userId, CommentPostRequest request);

    PagedCommentResponse getCommentsByVoteId(Long voteId, String sort, Pageable pageable);

    BestCommentResponseDto getBestCommentByVoteId(Long voteId);
}
