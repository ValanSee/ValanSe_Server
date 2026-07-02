package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.service.CommentLikeService.CommentLikeService;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.MemberService.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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
}
