package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.DailyVoteParticipation;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.DailyVoteWinner;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;
import com.valanse.valanse.repository.DailyVoteParticipationRepository;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.service.PointService.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyVoteSettlementServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 25);

    @Mock DailyVoteRepository dailyVoteRepository;
    @Mock DailyVoteParticipationRepository participationRepository;
    @Mock DailyVoteParticipationService participationService;
    @Mock PointService pointService;

    DailyVoteSettlementServiceImpl service;
    DailyVote dv;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T15:00:05Z"), ZoneId.of("Asia/Seoul"));
        service = new DailyVoteSettlementServiceImpl(
                dailyVoteRepository, participationRepository, participationService, pointService, clock);
        dv = DailyVote.builder().voteDate(DATE).voteId(10L).optionAId(100L).optionBId(200L).build();
        when(dailyVoteRepository.findById(DATE)).thenReturn(Optional.of(dv));
    }

    private DailyVoteParticipation part(long memberId, Long first, Long current, boolean bot) {
        return DailyVoteParticipation.builder()
                .voteDate(DATE).voteId(10L)
                .member(Member.builder().id(memberId).isBot(bot).build())
                .firstOptionId(first).currentOptionId(current).build();
    }

    @Test
    @DisplayName("A가_많으면_첫선택이_A인_참여자만_5포인트")
    void A가_많으면_첫선택이_A인_참여자만_5포인트() {
        when(participationService.tally(dv)).thenReturn(new DailyVoteTally(3, 1));
        DailyVoteParticipation stayedA = part(1, 100L, 100L, false);
        DailyVoteParticipation bToA = part(2, 200L, 100L, false);
        DailyVoteParticipation aToB = part(3, 100L, 200L, false);
        when(participationRepository.findByVoteDateAndVoteId(DATE, 10L)).thenReturn(List.of(stayedA, bToA, aToB));

        service.settle(DATE);

        assertThat(dv.getWinner()).isEqualTo(DailyVoteWinner.A);
        assertThat(stayedA.getBonusPoint()).isEqualTo(5);
        assertThat(aToB.getBonusPoint()).isEqualTo(5);
        assertThat(bToA.getBonusPoint()).isZero();
        verify(pointService).givePoint(1L, PointType.DAILY_WIN);
        verify(pointService).givePoint(3L, PointType.DAILY_WIN);
        verify(pointService, never()).givePoint(2L, PointType.DAILY_WIN);
    }

    @Test
    @DisplayName("동률이면_전원_10포인트")
    void 동률이면_전원_10포인트() {
        when(participationService.tally(dv)).thenReturn(new DailyVoteTally(2, 2));
        DailyVoteParticipation p1 = part(1, 100L, 100L, false);
        DailyVoteParticipation p2 = part(2, 200L, 200L, false);
        when(participationRepository.findByVoteDateAndVoteId(DATE, 10L)).thenReturn(List.of(p1, p2));

        service.settle(DATE);

        assertThat(dv.getWinner()).isEqualTo(DailyVoteWinner.TIE);
        assertThat(p1.getBonusPoint()).isEqualTo(10);
        assertThat(p2.getBonusPoint()).isEqualTo(10);
        verify(pointService).givePoint(1L, PointType.DAILY_TIE);
        verify(pointService).givePoint(2L, PointType.DAILY_TIE);
    }

    @Test
    @DisplayName("0대0이면_NONE이고_보너스없음")
    void 영대영이면_NONE이고_보너스없음() {
        when(participationService.tally(dv)).thenReturn(new DailyVoteTally(0, 0));

        service.settle(DATE);

        assertThat(dv.getWinner()).isEqualTo(DailyVoteWinner.NONE);
        assertThat(dv.getSettledAt()).isNotNull();
        verifyNoInteractions(pointService);
    }

    @Test
    @DisplayName("이미_정산된_날은_다시_지급하지_않는다")
    void 이미_정산된_날은_다시_지급하지_않는다() {
        dv.settle(1, 0, DailyVoteWinner.A, LocalDateTime.of(2026, 11, 26, 0, 0));

        assertThat(service.settle(DATE)).isEmpty();

        verifyNoInteractions(pointService);
        verify(participationRepository, never()).findByVoteDateAndVoteId(any(), anyLong());
    }

    @Test
    @DisplayName("정산값은_settledACount와_settledBCount에_저장된다")
    void 정산값은_settledACount와_settledBCount에_저장된다() {
        when(participationService.tally(dv)).thenReturn(new DailyVoteTally(1, 4));
        when(participationRepository.findByVoteDateAndVoteId(DATE, 10L)).thenReturn(List.of());

        Optional<DailyVote> result = service.settle(DATE);

        assertThat(result).containsSame(dv);
        assertThat(dv.getSettledACount()).isEqualTo(1);
        assertThat(dv.getSettledBCount()).isEqualTo(4);
        assertThat(dv.getWinner()).isEqualTo(DailyVoteWinner.B);
        assertThat(dv.getSettledAt()).isEqualTo(LocalDateTime.of(2026, 11, 26, 0, 0, 5));
    }

    @Test
    @DisplayName("교체된_날은_최종게임_참여자만_정산한다")
    void 교체된_날은_최종게임_참여자만_정산한다() {
        when(participationService.tally(dv)).thenReturn(new DailyVoteTally(1, 0));
        DailyVoteParticipation finalGame = part(1, 100L, 100L, false);
        when(participationRepository.findByVoteDateAndVoteId(DATE, dv.getVoteId())).thenReturn(List.of(finalGame));

        service.settle(DATE);

        verify(participationRepository).findByVoteDateAndVoteId(DATE, 10L);
        verify(pointService).givePoint(1L, PointType.DAILY_WIN);
        assertThat(finalGame.getBonusPoint()).isEqualTo(5);
    }

    @Test
    @DisplayName("봇과_첫선택이_없는_참여자는_보너스_0이고_지급하지_않는다")
    void 봇과_첫선택이_없는_참여자는_보너스_0이고_지급하지_않는다() {
        when(participationService.tally(dv)).thenReturn(new DailyVoteTally(2, 2));
        DailyVoteParticipation bot = part(1, 100L, 100L, true);
        DailyVoteParticipation purged = part(2, null, 200L, false);
        when(participationRepository.findByVoteDateAndVoteId(DATE, 10L)).thenReturn(List.of(bot, purged));

        service.settle(DATE);

        assertThat(bot.getBonusPoint()).isZero();
        assertThat(purged.getBonusPoint()).isZero();
        verifyNoInteractions(pointService);
    }

    @Test
    @DisplayName("게임이_삭제돼_voteId가_없으면_NONE으로_정산만_기록한다")
    void 게임이_삭제돼_voteId가_없으면_NONE으로_정산만_기록한다() {
        DailyVote purgedDv = DailyVote.builder().voteDate(DATE).build();
        when(dailyVoteRepository.findById(DATE)).thenReturn(Optional.of(purgedDv));

        service.settle(DATE);

        assertThat(purgedDv.getWinner()).isEqualTo(DailyVoteWinner.NONE);
        assertThat(purgedDv.getSettledAt()).isNotNull();
        verifyNoInteractions(pointService, participationService);
    }
}
