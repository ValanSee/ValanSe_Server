package com.valanse.valanse.service.BotVoteService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotVoteServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private MemberVoteOptionRepository memberVoteOptionRepository;

    @InjectMocks
    private BotVoteServiceImpl botVoteService;

    @Test
    @DisplayName("봇 투표는 MemberVoteOption만 저장하고 집계 카운트는 건드리지 않는다")
    void castBotVote_SavesRelationOnly() {
        // Given
        Long botMemberId = 1L;
        Long voteOptionId = 10L;
        Member botMember = Member.builder().id(botMemberId).isBot(true).build();
        Vote vote = Vote.builder().id(100L).totalVoteCount(0).build();
        VoteOption voteOption = VoteOption.builder().id(voteOptionId).vote(vote).voteCount(0).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(botMemberId)).thenReturn(Optional.of(botMember));
        when(voteOptionRepository.findById(voteOptionId)).thenReturn(Optional.of(voteOption));

        // When
        botVoteService.castBotVote(botMemberId, voteOptionId);

        // Then
        ArgumentCaptor<MemberVoteOption> captor = ArgumentCaptor.forClass(MemberVoteOption.class);
        verify(memberVoteOptionRepository).save(captor.capture());
        verify(memberVoteOptionRepository).flush();
        assertThat(captor.getValue().getMember()).isEqualTo(botMember);
        assertThat(captor.getValue().getVoteOption()).isEqualTo(voteOption);
        assertThat(captor.getValue().getVote()).isEqualTo(vote);

        // 집계 카운트는 그대로다 (증가 로직 자체가 없음을 값으로도 재확인)
        assertThat(vote.getTotalVoteCount()).isZero();
        assertThat(voteOption.getVoteCount()).isZero();
    }

    @Test
    @DisplayName("봇이 아닌 일반 회원 ID로 호출하면 예외를 던지고 저장하지 않는다")
    void castBotVote_MemberNotBot_ThrowsException() {
        // Given
        Long memberId = 2L;
        Long voteOptionId = 10L;
        Member humanMember = Member.builder().id(memberId).isBot(false).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(memberId)).thenReturn(Optional.of(humanMember));

        // When / Then
        assertThatThrownBy(() -> botVoteService.castBotVote(memberId, voteOptionId))
                .isInstanceOf(ApiException.class);
        verify(memberVoteOptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 투표한 봇이 다시 투표하면 예외를 던진다")
    void castBotVote_DuplicateVote_ThrowsException() {
        // Given
        Long botMemberId = 1L;
        Long voteOptionId = 10L;
        Member botMember = Member.builder().id(botMemberId).isBot(true).build();
        Vote vote = Vote.builder().id(100L).totalVoteCount(0).build();
        VoteOption voteOption = VoteOption.builder().id(voteOptionId).vote(vote).voteCount(0).build();

        when(memberRepository.findByIdAndDeletedAtIsNull(botMemberId)).thenReturn(Optional.of(botMember));
        when(voteOptionRepository.findById(voteOptionId)).thenReturn(Optional.of(voteOption));
        when(memberVoteOptionRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        // When / Then
        assertThatThrownBy(() -> botVoteService.castBotVote(botMemberId, voteOptionId))
                .isInstanceOf(ApiException.class);
    }
}
