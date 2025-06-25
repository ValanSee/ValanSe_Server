package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Comment.CommentRequest;
import com.valanse.valanse.dto.Comment.CommentResponse;
import com.valanse.valanse.service.CommentService.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3. 댓글 API", description = "댓글 작성 및 조회, 수정 관련 기능")
@RestController
@RequestMapping("/votes/{voteId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 작성",
            description = "댓글을 작성하는 API입니다."
    )
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable("voteId") Long voteId,
            @RequestBody CommentRequest request
    ) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Long commentId = commentService.createComment(voteId, userId, request);
        return ResponseEntity.ok(new CommentResponse(commentId));
    }
}

