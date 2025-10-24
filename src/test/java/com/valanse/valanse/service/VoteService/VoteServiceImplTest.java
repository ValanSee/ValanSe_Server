package com.valanse.valanse.service.VoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.Vote.VoteCancleResponseDto;
import com.valanse.valanse.dto.Vote.VoteCreateRequest;
import com.valanse.valanse.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @InjectMocks
    private VoteServiceImpl voteService;

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberVoteOptionRepository memberVoteOptionRepository;
    @Mock
    private CommentGroupRepository commentGroupRepository;
    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Test
    @DisplayName("투표 제목은 너무 길거나 너무 짧으면 안된다.")
    void 제목길이실패_test() {

        //given
        Member member = new Member();
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));

        // 주석 부분 바꿔가며 확인 가능
        VoteCreateRequest request = VoteCreateRequest.builder()
//                .title("123123123123123123123123123123")
                .title("")
                .build();

        // when
        ApiException exception = assertThrows(ApiException.class,
                () -> voteService.createVote(1L, request));

        // then
        assertThat(exception.getMessage()).isEqualTo("투표 제목은 1자 이상 25자 이하여야 합니다 (공백 제외).");
    }

    @Test
    @DisplayName("투표 옵션은 너무 많아도, 너무 적어도 안된다.")
    void 투표옵션실패_test() {
        // given
        Member member = new Member();
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));

        // 주석 부분 바꿔가며 테스트 가능
        VoteCreateRequest request = VoteCreateRequest.builder()
                .title("테스트용 투표")
//                .options(List.of("1번", "2번", "3번", "4번", "5번"))
                .options(null)
                .build();
        // when
        ApiException exception = assertThrows(ApiException.class,
                () -> voteService.createVote(1L, request));

        //then
        assertThat(exception.getMessage()).isEqualTo("투표 옵션은 1개 이상 4개 이하여야 합니다.");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void 투표생성_test() {
        // given
        Member member = new Member();

        VoteCreateRequest request = VoteCreateRequest.builder()
                .title("테스트용 투표")
                .options(List.of("1번", "2번", "3번", "4번"))
                .category(VoteCategory.ALL)
                .build();

        Vote vote = Vote.builder()
                .id(100L)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .member(member)
                .build();

        //stub
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.save(any())).thenReturn(vote);

        // when
        Long voteId = voteService.createVote(1L, request);

        // then
        assertThat(voteId).isEqualTo(100L);

        verify(voteRepository).save(any(Vote.class));
        verify(commentGroupRepository).save(any(CommentGroup.class));

        ArgumentCaptor<Vote> voteArgumentCaptor = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository).save(voteArgumentCaptor.capture());
        Vote captorVote = voteArgumentCaptor.getValue();
        assertThat(captorVote.getVoteOptions()).hasSize(4);
    }

    @Test
    void 처음투표하기_test() {
        // given
        Member member = Member.builder()
                .id(1L)
                .build();
        Vote vote = Vote.builder()
                .id(10L)
                .totalVoteCount(7)
                .build();

        VoteOption voteOption = VoteOption.builder()
                .id(100L)
                .voteCount(3)
                .vote(vote)
                .build();


        // stub
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(voteOption));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.empty());

        // when
        VoteCancleResponseDto responseDto = voteService.processVote(1L, 10L, 100L);

        // then : 응답 dto 속성 검증
        assertThat(responseDto.getTotalVoteCount()).isEqualTo(8);
        assertThat(responseDto.getVoteOptionCount()).isEqualTo(4);
        assertThat(responseDto.isVoted()).isTrue();
    }

    @Test
    void 투표취소_test() {

        // given
        Member member = Member.builder()
                .id(1L)
                .build();

        Vote vote = Vote.builder()
                .id(10L)
                .totalVoteCount(7)
                .build();

        VoteOption voteOption = VoteOption.builder()
                .id(100L)
                .voteCount(3)
                .vote(vote)
                .build();

        MemberVoteOption mvo = MemberVoteOption.builder()
                .voteOption(voteOption)
                .build();
        //stub
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(voteOption));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.of(mvo));

        //when
        VoteCancleResponseDto responseDto = voteService.processVote(1L, 10L, 100L);

        //then: dto 값 검증, memberVoteOption 삭제 호출 1회 검증
        assertThat(responseDto.getTotalVoteCount()).isEqualTo(6);
        assertThat(responseDto.getVoteOptionCount()).isEqualTo(2);
        assertThat(responseDto.isVoted()).isFalse();

        verify(memberVoteOptionRepository,times(1)).delete(any(MemberVoteOption.class));
    }

    @Test
    void 투표재선택_test(){

        // given
        Member member = Member.builder()
                .id(1L)
                .build();
        Vote vote = Vote.builder()
                .id(10L)
                .totalVoteCount(7)
                .build();

        VoteOption voteOption = VoteOption.builder()
                .id(100L)
                .voteCount(3)
                .vote(vote)
                .build();
        VoteOption newVoteOption = VoteOption.builder()
                .id(101L)
                .voteCount(10)
                .vote(vote)
                .build();

        MemberVoteOption mvo = MemberVoteOption.builder()
                .voteOption(voteOption)
                .build();
        //stub
        when(memberRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.of(member));
        when(voteRepository.findById(any())).thenReturn(Optional.of(vote));
        when(voteOptionRepository.findById(any())).thenReturn(Optional.of(newVoteOption));
        when(memberVoteOptionRepository.findByMemberIdAndVoteId(any(), any())).thenReturn(Optional.of(mvo));

        //when
        VoteCancleResponseDto responseDto = voteService.processVote(1L, 10L, 101L);

        //then: repository 동작, 응답 dto 속성 검증
        assertThat(responseDto.getTotalVoteCount()).isEqualTo(7);
        assertThat(responseDto.getVoteOptionCount()).isEqualTo(11);
        assertThat(responseDto.isVoted()).isTrue();

        verify(memberVoteOptionRepository, times(1)).delete(any(MemberVoteOption.class));
        verify(memberVoteOptionRepository, times(1)).save(any(MemberVoteOption.class));
        verify(voteOptionRepository, times(2)).save(any(VoteOption.class));
    }





}