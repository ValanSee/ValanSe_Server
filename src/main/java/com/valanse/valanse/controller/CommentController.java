package com.valanse.valanse.controller;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentController { // ← 반드시 클래스 내부여야 함

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    @Operation(summary = "내가 쓴 댓글 삭제")
    public ResponseEntity<Void> deleteMyComment(
            @AuthenticationPrincipal Member member,
            @PathVariable Long commentId) {

        commentService.deleteMyComment(member, commentId);
        return ResponseEntity.noContent().build();
    }
}
