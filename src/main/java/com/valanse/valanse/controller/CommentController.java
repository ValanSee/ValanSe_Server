package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final MemberService memberService;

    @DeleteMapping("/{commentId}")
    @Operation(summary = "내가 쓴 댓글 삭제")
    public ResponseEntity<Void> deleteMyComment(@PathVariable Long commentId) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);
        commentService.deleteMyComment(member, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-comments")
    @Operation(summary = "내가 쓴 댓글 목록 조회")
    public ResponseEntity<List<CommentResponseDto>> getMyComments() {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);
        List<CommentResponseDto> myComments = commentService.getMyComments(member);
        return ResponseEntity.ok(myComments);
    }
}
