package com.valanse.valanse.repository.CommentRepositoryCustom;


import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.dto.Comment.AdminCommentResponseDto;
import com.valanse.valanse.dto.Comment.CommentReplyResponseDto;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Comment.MyCommentResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

/**
 * DB 조회와 저장을 담당하는 레포지토리 코드입니다.
 */
public interface CommentRepositoryCustom {
    // loginId가 있으면 그 회원이 차단한 작성자의 댓글은 제외한다.
    Slice<CommentResponseDto> findCommentsByVoteIdSlice(Long voteId, String sort, Pageable pageable, Long loginId,boolean isAdmin);

    Slice<MyCommentResponseDto> findMyCommentsSlice(Long memberId, String sort, Pageable pageable);

    // loginId가 있으면 그 회원이 차단한 작성자의 대댓글은 제외한다.
    Slice<CommentReplyResponseDto> findRepliesByParentIdSlice(Long voteId, Long parentCommentId, Pageable pageable, Long loginId, boolean isAdmin);

    // blockerId(로그인 회원)가 차단한 작성자의 댓글은 제외한다. 비로그인이면 null.
    Optional<Comment> findMostLikedCommentByVoteId(Long voteId, Long blockerId);

    Long countActiveCommentsByVoteId(Long voteId);

    // 관리자(CMS) 콘텐츠 검수용: 부모 댓글과 대댓글을 하나의 목록으로, 최신순으로 조회.
    // isBot이 null이면 전체, true/false면 해당 작성자 유형만 필터링한다.
    Slice<AdminCommentResponseDto> findAdminCommentsSlice(Boolean isBot, Pageable pageable);
}
