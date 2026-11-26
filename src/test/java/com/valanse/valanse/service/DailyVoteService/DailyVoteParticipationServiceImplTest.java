package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.DailyVoteParticipation;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.DailyVote.DailyVoteReward;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;
import com.valanse.valanse.repository.DailyVoteParticipationRepository;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.service.PointService.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyVoteParticipationServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 26);

    @Mock DailyVoteRepository dailyVoteRepository;
    @Mock DailyVoteParticipationRepository participationRepository;
    @Mock PointService pointService;
    @Mock MemberProfileRepository memberProfileRepository;

    DailyVoteParticipationServiceImpl service;
    DailyVote today;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T15:00:05Z"), ZoneId.of("Asia/Seoul"));
        service = new DailyVoteParticipationServiceImpl(dailyVoteRepository, participationRepository, pointService,
                new DailyVotePointEligibility(memberProfileRepository), clock);
        lenient().when(memberProfileRepository.existsByMemberId(anyLong())).thenReturn(true);
        today = DailyVote.builder().voteDate(DATE).voteId(10L).optionAId(100L).optionBId(200L).build();
    }

    private Member member(boolean bot) {
        return Member.builder().id(1L).isBot(bot).build();
    }

    @Test
    @DisplayName("첫_참여면_3포인트를_주고_기록한다")
    void 첫_참여면_3포인트를_주고_기록한다() {
        when(participationRepository.existsByVoteDateAndMember_IdAndParticipationPointGreaterThan(DATE, 1L, 0)).thenReturn(false);

        DailyVoteReward reward = service.participate(today, member(false), 100L);

        assertThat(reward).isEqualTo(new DailyVoteReward(3, false));
        verify(pointService).givePoint(1L, PointType.DAILY_VOTE);
        ArgumentCaptor<DailyVoteParticipation> cap = ArgumentCaptor.forClass(DailyVoteParticipation.class);
        verify(participationRepository).save(cap.capture());
        DailyVoteParticipation saved = cap.getValue();
        assertThat(saved.getFirstOptionId()).isEqualTo(100L);
        assertThat(saved.getCurrentOptionId()).isEqualTo(100L);
        assertThat(saved.getParticipationPoint()).isEqualTo(3);
        assertThat(saved.getVoteDate()).isEqualTo(DATE);
        assertThat(saved.getVoteId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("같은날_다른게임에_참여해도_포인트는_한번")
    void 같은날_다른게임에_참여해도_포인트는_한번() {
        when(participationRepository.existsByVoteDateAndMember_IdAndParticipationPointGreaterThan(DATE, 1L, 0)).thenReturn(true);

        DailyVoteReward reward = service.participate(today, member(false), 200L);

        assertThat(reward).isEqualTo(new DailyVoteReward(0, true));
        verify(pointService, never()).givePoint(anyLong(), any());
        ArgumentCaptor<DailyVoteParticipation> cap = ArgumentCaptor.forClass(DailyVoteParticipation.class);
        verify(participationRepository).save(cap.capture());
        assertThat(cap.getValue().getParticipationPoint()).isZero();
    }

    @Test
    @DisplayName("봇은_포인트_없이_0으로_기록한다")
    void 봇은_포인트_없이_0으로_기록한다() {
        when(participationRepository.existsByVoteDateAndMember_IdAndParticipationPointGreaterThan(DATE, 1L, 0)).thenReturn(false);

        DailyVoteReward reward = service.participate(today, member(true), 100L);

        assertThat(reward).isEqualTo(new DailyVoteReward(0, false));
        verify(pointService, never()).givePoint(anyLong(), any());
        ArgumentCaptor<DailyVoteParticipation> cap = ArgumentCaptor.forClass(DailyVoteParticipation.class);
        verify(participationRepository).save(cap.capture());
        assertThat(cap.getValue().getParticipationPoint()).isZero();
    }

    @Test
    @DisplayName("tally는_current_option_기준으로_센다")
    void tally는_current_option_기준으로_센다() {
        when(participationRepository.countByVoteDateAndVoteIdAndCurrentOptionId(DATE, 10L, 100L)).thenReturn(7L);
        when(participationRepository.countByVoteDateAndVoteIdAndCurrentOptionId(DATE, 10L, 200L)).thenReturn(4L);

        assertThat(service.tally(today)).isEqualTo(new DailyVoteTally(7, 4));
    }

    @Test
    @DisplayName("findTodayFor는_clock의_KST_날짜를_쓴다")
    void findTodayFor는_clock의_KST_날짜를_쓴다() {
        when(dailyVoteRepository.findByVoteDateAndVoteId(DATE, 10L)).thenReturn(Optional.of(today));

        assertThat(service.findTodayFor(10L)).contains(today);
        verify(dailyVoteRepository).findByVoteDateAndVoteId(eq(LocalDate.of(2026, 11, 26)), eq(10L));
    }

    @Test
    @DisplayName("updateCurrentOption은_현재_선택만_바꾸고_첫_선택은_유지한다")
    void updateCurrentOption은_현재_선택만_바꾸고_첫_선택은_유지한다() {
        DailyVoteParticipation p = DailyVoteParticipation.builder()
                .voteDate(DATE).voteId(10L).firstOptionId(100L).currentOptionId(100L).build();
        when(participationRepository.findByVoteDateAndVoteIdAndMember_Id(DATE, 10L, 1L)).thenReturn(Optional.of(p));

        service.updateCurrentOption(today, 1L, null);

        assertThat(p.getCurrentOptionId()).isNull();
        assertThat(p.getFirstOptionId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("프로필이_없는_회원은_참여포인트_0")
    void 프로필이_없는_회원은_참여포인트_0() {
        when(memberProfileRepository.existsByMemberId(1L)).thenReturn(false);
        when(participationRepository.existsByVoteDateAndMember_IdAndParticipationPointGreaterThan(DATE, 1L, 0)).thenReturn(false);

        DailyVoteReward reward = service.participate(today, member(false), 100L);

        assertThat(reward).isEqualTo(new DailyVoteReward(0, false));
        verify(pointService, never()).givePoint(anyLong(), any());
        ArgumentCaptor<DailyVoteParticipation> cap = ArgumentCaptor.forClass(DailyVoteParticipation.class);
        verify(participationRepository).save(cap.capture());
        assertThat(cap.getValue().getParticipationPoint()).isZero();
    }
}
