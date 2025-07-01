package com.valanse.valanse.controller;

import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final MemberService memberService;

    @DeleteMapping("/{commentId}")
    @Operation(summary = "내가 쓴 댓글 삭제")
    public ResponseEntity<Void> deleteMyComment(@PathVariable Long commentId) {
        // 🔒 현재 로그인된 사용자 ID를 가져오기
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());

        // 🔍 사용자 정보 조회
        var member = memberService.findById(loginId);

        // 🧹 삭제 실행
        commentService.deleteMyComment(member, commentId);
        return ResponseEntity.noContent().build();
    }
}
