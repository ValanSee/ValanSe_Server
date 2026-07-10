package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.dto.Comment.PagedCommentReplyResponse;
import com.valanse.valanse.service.CommentLikeService.CommentLikeService;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentControllerPaginationTest {

    private final CommentController controller = new CommentController(
            mock(CommentService.class),
            mock(CommentLikeService.class),
            mock(MemberService.class)
    );

    @Test
    @DisplayName("댓글 목록의 page가 음수이면 400 예외를 발생시킨다")
    void getComments_NegativePage_ThrowsBadRequest() {
        assertThatThrownBy(() -> controller.getComments(1L, "latest", -1, 10))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("page는 0 이상이어야 합니다.");
                });
    }

    @Test
    @DisplayName("댓글 목록의 size가 50보다 크면 400 예외를 발생시킨다")
    void getComments_SizeOverMaximum_ThrowsBadRequest() {
        assertThatThrownBy(() -> controller.getComments(1L, "latest", 0, 51))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("size는 1 이상 50 이하여야 합니다.");
                });
    }

    @Test
    @DisplayName("대댓글 목록의 page가 음수이면 400 예외를 발생시킨다")
    void getReplies_NegativePage_ThrowsBadRequest() {
        assertThatThrownBy(() -> controller.getReplies(1L, 2L, -1, 10))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("page는 0 이상이어야 합니다.");
                });
    }

    @Test
    @DisplayName("대댓글 목록 조회는 page와 size를 Pageable로 전달한다")
    void getReplies_UsesPageable() {
        CommentService commentService = mock(CommentService.class);
        CommentController controller = new CommentController(
                commentService,
                mock(CommentLikeService.class),
                mock(MemberService.class)
        );
        PagedCommentReplyResponse response = PagedCommentReplyResponse.builder()
                .page(1)
                .size(10)
                .hasNext(false)
                .build();
        when(commentService.getReplies(isNull(), eq(1L), eq(2L), any(Pageable.class)))
                .thenReturn(response);

        ResponseEntity<PagedCommentReplyResponse> result = controller.getReplies(1L, 2L, 1, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(commentService).getReplies(isNull(), eq(1L), eq(2L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(result.getBody()).isSameAs(response);
    }
}
