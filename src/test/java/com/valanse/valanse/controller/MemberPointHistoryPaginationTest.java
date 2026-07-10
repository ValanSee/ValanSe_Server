package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.dto.PointHistory.PointHistoryResponse;
import com.valanse.valanse.service.MemberProfileService.MemberProfileService;
import com.valanse.valanse.service.PointService.PointService;
import com.valanse.valanse.service.TitleService.TitleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberPointHistoryPaginationTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("포인트 히스토리 page가 음수이면 400 예외를 발생시킨다")
    void getPointHistory_NegativePage_ThrowsBadRequest() {
        MemberController controller = new MemberController(
                mock(MemberProfileService.class),
                mock(PointService.class),
                mock(TitleService.class)
        );

        assertThatThrownBy(() -> controller.getPointHistory(-1, 10))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("page는 0 이상이어야 합니다.");
                });
    }

    @Test
    @DisplayName("포인트 히스토리 조회는 page와 size를 Pageable로 전달한다")
    void getPointHistory_UsesPageable() {
        PointService pointService = mock(PointService.class);
        MemberController controller = new MemberController(
                mock(MemberProfileService.class),
                pointService,
                mock(TitleService.class)
        );
        PointHistoryResponse response = new PointHistoryResponse(List.of(), 1, 10, false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null)
        );
        when(pointService.getPointHistory(eq(1L), any(Pageable.class))).thenReturn(response);

        ResponseEntity<PointHistoryResponse> result = controller.getPointHistory(1, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(pointService).getPointHistory(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(result.getBody()).isSameAs(response);
    }
}
