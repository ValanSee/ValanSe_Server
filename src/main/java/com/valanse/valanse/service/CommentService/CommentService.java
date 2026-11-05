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

    PagedMyCommentResponse getMyComments(Member member, String sort, Pageable pageable);

    Long createComment(Long voteId, Long userId, CommentPostRequest request);

    PagedCommentResponse getCommentsByVoteId(Long voteId, String sort, Pageable pageable,Long userId, Boolean isAdmin);
    BestCommentResponseDto getBestCommentByVoteId(Long voteId);
    PagedCommentReplyResponse getReplies(Member loginUser, Long voteId, Long commentId, Pageable pageable);

    // 관리자(CMS) 콘텐츠 검수용 댓글 통합 목록 조회.
    PagedAdminCommentResponse getAdminComments(Boolean isBot, Pageable pageable);
}
