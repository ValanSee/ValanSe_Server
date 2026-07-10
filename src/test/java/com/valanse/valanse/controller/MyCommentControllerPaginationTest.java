package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Comment.PagedMyCommentResponse;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyCommentControllerPaginationTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("내 댓글 목록의 page가 음수이면 400 예외를 발생시킨다")
    void getMyComments_NegativePage_ThrowsBadRequest() {
        MyCommentController controller = new MyCommentController(
                mock(CommentService.class),
                mock(MemberService.class)
        );

        assertThatThrownBy(() -> controller.getMyComments("latest", -1, 10))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("page는 0 이상이어야 합니다.");
                });
    }

    @Test
    @DisplayName("내 댓글 목록 조회는 page와 size를 Pageable로 전달한다")
    void getMyComments_UsesPageable() {
        CommentService commentService = mock(CommentService.class);
        MemberService memberService = mock(MemberService.class);
        MyCommentController controller = new MyCommentController(commentService, memberService);
        Member member = Member.builder().id(1L).build();
        PagedMyCommentResponse response = PagedMyCommentResponse.builder()
                .page(1)
                .size(10)
                .hasNext(false)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null)
        );
        when(memberService.findById(1L)).thenReturn(member);
        when(commentService.getMyComments(eq(member), eq("latest"), any(Pageable.class)))
                .thenReturn(response);

        ResponseEntity<PagedMyCommentResponse> result = controller.getMyComments("latest", 1, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentService).getMyComments(eq(member), eq("latest"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(result.getBody()).isSameAs(response);
    }
}
