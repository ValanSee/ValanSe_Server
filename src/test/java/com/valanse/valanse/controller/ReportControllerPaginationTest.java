package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.dto.Report.PagedReportedTargetResponse;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.ReportService.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerPaginationTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("신고 목록의 page가 음수이면 400 예외를 발생시킨다")
    void getReportedTargets_NegativePage_ThrowsBadRequest() {
        ReportController controller = new ReportController(
                mock(ReportService.class),
                mock(MemberService.class)
        );

        assertThatThrownBy(() -> controller.getReportedTargets(ReportType.VOTE, "latest", -1, 10))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("page는 0 이상이어야 합니다.");
                });
    }

    @Test
    @DisplayName("신고 목록 조회는 page와 size를 Pageable로 전달한다")
    void getReportedTargets_UsesPageable() {
        ReportService reportService = mock(ReportService.class);
        ReportController controller = new ReportController(
                reportService,
                mock(MemberService.class)
        );
        PagedReportedTargetResponse response = PagedReportedTargetResponse.builder()
                .page(1)
                .size(10)
                .hasNext(false)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
        when(reportService.getReportedTargets(any(), eq(ReportType.VOTE), eq("latest"), any(Pageable.class)))
                .thenReturn(response);

        ResponseEntity<PagedReportedTargetResponse> result =
                controller.getReportedTargets(ReportType.VOTE, "latest", 1, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reportService).getReportedTargets(any(), eq(ReportType.VOTE), eq("latest"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(result.getBody()).isSameAs(response);
    }
}
