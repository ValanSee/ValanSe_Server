package com.valanse.valanse.repository.CommentRepositoryCustom;


import com.valanse.valanse.dto.Comment.CommentResponseDto;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CommentRepositoryCustom {
    List<CommentResponseDto> findCommentsByVoteIdOrderBy(Long voteId, String sort, Pageable pageable);
}

