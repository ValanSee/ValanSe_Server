package com.valanse.valanse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.dto.Vote.PagedVoteResponse;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.VoteService.VoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoteMinePaginationTest {

    @Test
    @DisplayName("내가 만든 투표 목록의 page가 음수이면 400 예외를 발생시킨다")
    void getMyCreatedVotes_NegativePage_ThrowsBadRequest() {
        VoteController controller = new VoteController(
                mock(VoteService.class),
                mock(MemberService.class),
                mock(ObjectMapper.class)
        );
        UserDetails userDetails = mock(UserDetails.class);

        assertThatThrownBy(() -> controller.getMyCreatedVotes(userDetails, "ALL", "latest", -1, 10))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("page는 0 이상이어야 합니다.");
                });
    }

    @Test
    @DisplayName("내가 만든 투표 목록 조회는 page와 size를 Pageable로 전달한다")
    void getMyCreatedVotes_UsesPageable() {
        VoteService voteService = mock(VoteService.class);
        VoteController controller = new VoteController(
                voteService,
                mock(MemberService.class),
                mock(ObjectMapper.class)
        );
        UserDetails userDetails = mock(UserDetails.class);
        PagedVoteResponse response = PagedVoteResponse.builder()
                .page(1)
                .size(10)
                .hasNext(false)
                .build();

        when(userDetails.getUsername()).thenReturn("1");
        when(voteService.getMyCreatedVotes(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(response);

        ResponseEntity<PagedVoteResponse> result =
                controller.getMyCreatedVotes(userDetails, "ALL", "latest", 1, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voteService).getMyCreatedVotes(eq(1L), eq("latest"), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("내가 투표한 목록 조회는 page와 size를 Pageable로 전달한다")
    void getMyVotedVotes_UsesPageable() {
        VoteService voteService = mock(VoteService.class);
        VoteController controller = new VoteController(
                voteService,
                mock(MemberService.class),
                mock(ObjectMapper.class)
        );
        UserDetails userDetails = mock(UserDetails.class);
        PagedVoteResponse response = PagedVoteResponse.builder()
                .page(2)
                .size(5)
                .hasNext(true)
                .build();

        when(userDetails.getUsername()).thenReturn("1");
        when(voteService.getMyVotedVotes(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(response);

        ResponseEntity<PagedVoteResponse> result =
                controller.getMyVotedVotes(userDetails, "ALL", "oldest", 2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(voteService).getMyVotedVotes(eq(1L), eq("oldest"), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(result.getBody()).isSameAs(response);
    }
}
