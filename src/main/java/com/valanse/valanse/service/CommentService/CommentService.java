package com.valanse.valanse.service.CommentService;

import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.domain.Member;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * CommentService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface CommentService {
    void deleteMyComment(Member member, Long commentId);

    List<MyCommentResponseDto> getMyComments(Member member, String sort);

    Long createComment(Long voteId, Long userId, CommentPostRequest request);

    PagedCommentResponse getCommentsByVoteId(Long voteId, String sort, Pageable pageable,Long userId, Boolean isAdmin);
    BestCommentResponseDto getBestCommentByVoteId(Long voteId);
    List<CommentReplyResponseDto> getReplies(Member loginUser, Long voteId, Long commentId);
}
