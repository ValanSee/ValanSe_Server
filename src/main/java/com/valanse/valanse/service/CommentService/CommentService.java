package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.domain.Member;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    void deleteMyComment(Member member, Long commentId);

    List<MyCommentResponseDto> getMyComments(Member member, String sort);

    Long createComment(Long voteId, Long userId, CommentPostRequest request);

    PagedCommentResponse getCommentsByVoteId(Long voteId, String sort, Pageable pageable);
    BestCommentResponseDto getBestCommentByVoteId(Long voteId);
    List<CommentReplyResponseDto> getReplies(Long voteId, Long commentId);
}
