package com.valanse.valanse.controller;

import com.valanse.valanse.common.auth.SecurityUtils;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.service.CommentLikeService.CommentLikeService;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "댓글 API", description = "댓글 작성 및 조회, 수정 관련 기능")
@RestController
@RequestMapping("/votes/{voteId}/comments")
@RequiredArgsConstructor
/**
 * 투표별 댓글, 대댓글, 좋아요 요청을 처리하는 컨트롤러 코드입니다.
 */
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final MemberService  memberService;

    @Operation(
            summary = "댓글 작성",
            description = "댓글을 작성하는 API입니다."
    )
    /**
     * 투표에 부모 댓글 또는 대댓글을 작성하고 댓글 카운트와 포인트를 갱신하는 메서드입니다.
     * check: 대댓글 parent가 현재 투표의 댓글인지 확인해야 합니다.
     */
    @PostMapping
    public ResponseEntity<CommentPostResponse> createComment(
            @PathVariable("voteId") Long voteId,
            @RequestBody CommentPostRequest request
    ) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Long commentId = commentService.createComment(voteId, userId, request);
        return ResponseEntity.ok(new CommentPostResponse(commentId));
    }

    @Operation(
            summary = "댓글 조회",
            description = "댓글을 조회하는 API입니다."
    )
    /**
     * Comments 정보를 조회하는 메서드입니다.
     */
    @GetMapping
    public PagedCommentResponse getComments(
            @PathVariable("voteId") Long voteId,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long loginId = null;
        boolean isAdmin = false;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            loginId = Long.parseLong(auth.getName());
            if (SecurityUtils.isCurrentUserAdmin()) {
                isAdmin = true;
            } else {
                Member member = memberService.findById(loginId);
                isAdmin = member.getRole() == Role.ADMIN;
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        return commentService.getCommentsByVoteId(voteId, sort, pageable,loginId,isAdmin);
    }

    @Operation(
            summary = "댓글 썸네일 조회",
            description = "해당 투표에 달린 댓글 중 좋아요 수가 가장 많은 댓글을 조회합니다."
    )
    /**
     * BestComment 정보를 조회하는 메서드입니다.
     */
    @GetMapping("/best")
    public BestCommentResponseDto getBestComment(@PathVariable("voteId") Long voteId) {
        return commentService.getBestCommentByVoteId(voteId);
    }

    @Operation(
            summary = "댓글 좋아요 수정",
            description = "댓글의 좋아요를 누르거나 취소하는 API입니다."
    )
    /**
     * 댓글 좋아요를 토글하고 좋아요 수를 갱신하는 메서드입니다.
     * check: 댓글이 현재 투표에 속하는지 검증해야 합니다.
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeResponseDto> likeComment(
            @PathVariable("voteId") Long voteId,
            @PathVariable("commentId") Long commentId
    ) {
        CommentLikeResponseDto response = commentLikeService.likeComment(voteId, commentId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "대댓글 조회",
            description = "대댓글을 조회하는 API입니다."
    )
    /**
     * 특정 부모 댓글의 대댓글 목록과 작성자/삭제 가능 여부 정보를 조회하는 메서드입니다.
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentReplyResponseDto>> getReplies(
            @PathVariable("voteId") Long voteId,
            @PathVariable("commentId") Long commentId
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Member member = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            if (SecurityUtils.isCurrentUserAdmin()) {
                member = Member.builder()
                        .id(0L)
                        .role(Role.ADMIN)
                        .build();
            } else {
                Long loginId = Long.parseLong(auth.getName());
                member = memberService.findById(loginId);
            }
        }

        List<CommentReplyResponseDto> replies = commentService.getReplies(member, voteId, commentId);
        return ResponseEntity.ok(replies);
    }
}

