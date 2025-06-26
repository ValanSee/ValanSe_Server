package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    Long createComment(Long voteId, Long userId, CommentPostRequest request);

    List<CommentResponseDto> getCommentsByVoteId(Long voteId, String sort, Pageable pageable);

}
