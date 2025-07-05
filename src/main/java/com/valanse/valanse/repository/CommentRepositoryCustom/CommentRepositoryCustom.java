package com.valanse.valanse.repository.CommentRepositoryCustom;


import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface CommentRepositoryCustom {
    Slice<CommentResponseDto> findCommentsByVoteIdSlice(Long voteId, String sort, Pageable pageable);

    Optional<Comment> findMostLikedCommentByVoteId(Long voteId);
}

