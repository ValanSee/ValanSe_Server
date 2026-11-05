package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.PaginationValidator;
import com.valanse.valanse.dto.Comment.PagedAdminCommentResponse;
import com.valanse.valanse.service.CommentService.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 댓글 검수 API", description = "CMS에서 AI 생성 댓글을 검수하기 위한 관리자 전용 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(
            summary = "관리자 댓글 검수 목록 조회",
            description = "부모 댓글과 대댓글을 하나의 목록으로 최신순 조회합니다. isBot을 지정하지 않으면 전체, "
                    + "true/false면 해당 작성자 유형만 조회합니다. 삭제된 댓글과 삭제된 투표에 속한 댓글은 제외됩니다."
    )
    public PagedAdminCommentResponse getAdminComments(
            @RequestParam(name = "isBot", required = false) Boolean isBot,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size
    ) {
        PaginationValidator.validatePageAndSize(page, size);

        Pageable pageable = PageRequest.of(page, size);
        return commentService.getAdminComments(isBot, pageable);
    }
}
