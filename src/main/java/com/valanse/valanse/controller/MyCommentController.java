package com.valanse.valanse.controller;

import com.valanse.valanse.common.auth.SecurityUtils;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.dto.Comment.*;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

import java.util.List;

@Tag(name = "내 댓글 API", description = "내가 작성한 댓글 조회 및 삭제 기능")
@RestController
@RequiredArgsConstructor
/**
 * 사용자가 작성한 댓글 목록과 삭제 요청을 처리하는 컨트롤러 코드입니다.
 */
public class MyCommentController {

    private final CommentService commentService;
    private final MemberService memberService;

    // 3. 내가 쓴 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    @Operation(
            summary = "내가 쓴 댓글 삭제",
            description = "내가 쓴 댓글을 삭제합니다. 지우고 싶은 commentId를 입력하면 해당 댓글이 삭제됩니다."
    )
    /**
     * 댓글 작성자 또는 관리자가 댓글을 소프트 삭제하고 관련 카운트를 감소시키는 메서드입니다.
     */
    public ResponseEntity<?> deleteMyComment(@PathVariable Long commentId) {
        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Member member = SecurityUtils.isCurrentUserAdmin()
                ? Member.builder().id(loginId).role(Role.ADMIN).build()
                : memberService.findById(loginId);
        commentService.deleteMyComment(member, commentId);
        return ResponseEntity.ok().body(Collections.singletonMap("message", "댓글이 삭제되었습니다."));
    }

    // 4. 내가 쓴 댓글 목록 조회
    @GetMapping("/comments/mine")
    @Operation(summary = "내가 쓴 댓글 목록 조회",
    description = "내가 쓴 댓글을 목록을 조회합니다. 내가 작성한 댓글을 시간순(latest/oldest)로 반환합니다."
            )
    /**
     * 사용자가 작성한 댓글 목록을 최신순 또는 오래된순으로 조회하는 메서드입니다.
     */
    public ResponseEntity<List<MyCommentResponseDto>> getMyComments(
            @RequestParam(defaultValue = "latest") String sort) {

        Long loginId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        var member = memberService.findById(loginId);

        List<MyCommentResponseDto> myComments = commentService.getMyComments(member, sort);

        return ResponseEntity.ok(myComments);
    }


}
