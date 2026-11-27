package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.common.alert.DailyVoteEvent;
import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.repository.DailyVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 오늘의 밸런스로 쓰이는 게임이 삭제됐을 때 오늘 행은 교체하고 예약 행은 풀어 주는 서비스 코드입니다.
 * 지난 날짜 행은 스냅샷 보존을 위해 건드리지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class DailyVoteLifecycleService {

    private final DailyVoteRepository dailyVoteRepository;
    private final DailyVoteSelector selector;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 게임 soft delete 와 같은 트랜잭션에서 호출되어, 오늘 행은 새 게임으로 교체하고 내일 이후 행은 삭제합니다.
     */
    @Transactional
    public void onVoteDeleted(Long voteId) {
        LocalDate today = LocalDate.now(clock);
        for (DailyVote row : dailyVoteRepository.findByVoteIdAndVoteDateGreaterThanEqual(voteId, today)) {
            if (row.getVoteDate().equals(today)) {
                replaceToday(row);
            } else {
                cancelReservation(row);
            }
        }
    }

    // 후보가 없으면 행을 그대로 두어(스냅샷 유지) 알림만 보낸다
    private void replaceToday(DailyVote row) {
        selector.pickCandidate().ifPresentOrElse(
                candidate -> {
                    String previousQuestion = row.getQuestionSnapshot();
                    row.assign(candidate, DailyVoteSource.AUTO);
                    dailyVoteRepository.save(row);
                    eventPublisher.publishEvent(DailyVoteEvent.replaced(
                            row.getVoteDate(), candidate.getId(), row.getQuestionSnapshot(),
                            DailyVoteSource.AUTO.name(), "삭제된 게임 \"" + previousQuestion + "\" 에서 교체"));
                },
                () -> eventPublisher.publishEvent(DailyVoteEvent.noCandidate(row.getVoteDate())));
    }

    private void cancelReservation(DailyVote row) {
        dailyVoteRepository.delete(row);
        eventPublisher.publishEvent(DailyVoteEvent.reservationCancelled(
                row.getVoteDate(), row.getVoteId(), row.getQuestionSnapshot(), "예약된 게임이 삭제되어 예약 해제"));
    }
}
