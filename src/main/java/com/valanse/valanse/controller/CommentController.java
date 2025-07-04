package com.valanse.valanse.controller;

import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.dto.Comment.CommentPostResponse;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Comment.PagedCommentResponse;
import com.valanse.valanse.service.CommentService.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<CommentPostResponse> createComment(
            @PathVariable("voteId") Long voteId,
            @RequestBody CommentPostRequest request
    ) {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Long commentId = commentService.createComment(voteId, userId, request);
        return ResponseEntity.ok(new CommentPostResponse(commentId));
    }

    @GetMapping
    public PagedCommentResponse getComments(
            @PathVariable("voteId") Long voteId,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return commentService.getCommentsByVoteId(voteId, sort, pageable);
    }
}

