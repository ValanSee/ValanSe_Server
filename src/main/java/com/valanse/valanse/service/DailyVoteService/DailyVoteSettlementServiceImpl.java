package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.DailyVoteParticipation;
import com.valanse.valanse.domain.enums.DailyVoteWinner;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;
import com.valanse.valanse.repository.DailyVoteParticipationRepository;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.service.PointService.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/**
 * 날짜별 오늘의 밸런스를 정산해 승자를 정하고, 첫 선택이 이긴 쪽인 참여자(무승부면 전원)에게 보너스 포인트를 지급하는 서비스 코드입니다.
 */
public class DailyVoteSettlementServiceImpl implements DailyVoteSettlementService {

    private static final int WIN_POINT = 5;
    private static final int TIE_POINT = 10;

    private final DailyVoteRepository dailyVoteRepository;
    private final DailyVoteParticipationRepository participationRepository;
    private final DailyVoteParticipationService participationService;
    private final PointService pointService;
    private final DailyVotePointEligibility pointEligibility;
    private final Clock clock;

    @Override
    @Transactional
    public Optional<DailyVote> settle(LocalDate date) {
        Optional<DailyVote> found = dailyVoteRepository.findById(date);
        if (found.isEmpty() || found.get().getSettledAt() != null) {
            return Optional.empty();
        }
        DailyVote dv = found.get();
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC)); // 다른 timestamp 컬럼과 같은 UTC

        // 게임이 삭제돼 voteId 가 비었으면 표 0:0 으로 정산(승자 없음, 보너스 없음)만 기록한다
        if (dv.getVoteId() == null) {
            dv.settle(0, 0, DailyVoteWinner.NONE, now);
            return Optional.of(dv);
        }

        DailyVoteTally tally = participationService.tally(dv);
        DailyVoteWinner winner = decideWinner(tally);
        dv.settle((int) tally.aCount(), (int) tally.bCount(), winner, now);

        if (winner != DailyVoteWinner.NONE) {
            List<DailyVoteParticipation> participants =
                    participationRepository.findByVoteDateAndVoteId(date, dv.getVoteId());
            for (DailyVoteParticipation p : participants) {
                grantBonus(dv, winner, p);
            }
        }
        return Optional.of(dv);
    }

    private static DailyVoteWinner decideWinner(DailyVoteTally tally) {
        if (tally.aCount() == tally.bCount()) {
            return tally.aCount() > 0 ? DailyVoteWinner.TIE : DailyVoteWinner.NONE;
        }
        return tally.aCount() > tally.bCount() ? DailyVoteWinner.A : DailyVoteWinner.B;
    }

    private void grantBonus(DailyVote dv, DailyVoteWinner winner, DailyVoteParticipation p) {
        PointType type = bonusType(dv, winner, p.getFirstOptionId());
        // 봇·탈퇴·프로필 없는 회원은 지급 대상이 아니다. givePoint 예외는 정산 트랜잭션 전체를 롤백시키므로 미리 걸러 0으로 기록한다
        if (type == null || !pointEligibility.canReceive(p.getMember())) {
            p.recordBonus(0);
            return;
        }
        pointService.givePoint(p.getMember().getId(), type);
        p.recordBonus(type == PointType.DAILY_TIE ? TIE_POINT : WIN_POINT);
    }

    /**
     * 첫 선택이 없으면(선택지 삭제) 보너스 대상이 아니다.
     */
    private static PointType bonusType(DailyVote dv, DailyVoteWinner winner, Long firstOptionId) {
        if (firstOptionId == null) {
            return null;
        }
        return switch (winner) {
            case TIE -> PointType.DAILY_TIE;
            case A -> firstOptionId.equals(dv.getOptionAId()) ? PointType.DAILY_WIN : null;
            case B -> firstOptionId.equals(dv.getOptionBId()) ? PointType.DAILY_WIN : null;
            case NONE -> null;
        };
    }
}
