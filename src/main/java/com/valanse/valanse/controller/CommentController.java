package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.valanse.valanse.dto.Comment.CommentResponseDto;


import java.util.List;

@Tag(name = "3. 댓글 API", description = "댓글 작성 및 조회, 수정 관련 기능")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final MemberService memberService;

    // 1. 투표글에 댓글 작성

    // 2. 투표글의 댓글 목록 조회

    // 3. 내가 쓴 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "내가 쓴 댓글 삭제")
    public ResponseEntity<Void> deleteMyComment(@PathVariable Long commentId) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);
        commentService.deleteMyComment(member, commentId);
        return ResponseEntity.noContent().build();
    }

    // 4. 내가 쓴 댓글 목록 조회
    @GetMapping("/comments/my-comments")
    @Operation(summary = "내가 쓴 댓글 목록 조회")
    public ResponseEntity<List<MyCommentResponseDto>> getMyComments(
            @RequestParam(defaultValue = "desc") String sort) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);
        List<MyCommentResponseDto> myComments = commentService.getMyComments(member, sort);
        return ResponseEntity.ok(myComments);
    }
}
