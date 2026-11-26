package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.common.alert.DailyVoteEvent;
import com.valanse.valanse.common.config.DailyVoteProperties;
import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.repository.DailyVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 오늘의 밸런스를 전날 23:30 에 선정하고, 자정(과 매시 안전망)에 미정산 지난 날짜를 정산한 뒤 오늘이 비어 있으면 채우는 스케줄러 코드입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyVoteScheduler {

    public static final String SELECT_LOCK_NAME = "dailyVoteSelect";
    public static final String SETTLE_LOCK_NAME = "dailyVoteSettle";
    public static final String SAFETY_NET_LOCK_NAME = "dailyVoteSafetyNet";

    private final DailyVoteSelector selector;
    private final DailyVoteSettlementService settlementService;
    private final DailyVoteRepository dailyVoteRepository;
    private final DailyVoteProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Scheduled(cron = "${daily-vote.select-cron:0 30 23 * * *}", zone = "${daily-vote.zone:Asia/Seoul}")
    @SchedulerLock(name = SELECT_LOCK_NAME, lockAtMostFor = "PT30M")
    public void selectTomorrow() {
        if (!properties.isEnabled()) return;
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        try {
            ensureAndNotify(tomorrow);
        } catch (Exception exception) {
            log.error("오늘의 밸런스 선정 실패. date={}", tomorrow, exception);
        }
    }

    @Scheduled(cron = "${daily-vote.settle-cron:0 0 0 * * *}", zone = "${daily-vote.zone:Asia/Seoul}")
    @SchedulerLock(name = SETTLE_LOCK_NAME, lockAtMostFor = "PT30M")
    public void settleAndEnsureToday() {
        if (!properties.isEnabled()) return;
        settleAllPastAndEnsureToday();
    }

    // 자정 크론이 실패하거나 누락돼도 매시 복구한다
    @Scheduled(cron = "${daily-vote.safety-cron:0 5 * * * *}", zone = "${daily-vote.zone:Asia/Seoul}")
    @SchedulerLock(name = SAFETY_NET_LOCK_NAME, lockAtMostFor = "PT30M")
    public void hourlySafetyNet() {
        if (!properties.isEnabled()) return;
        settleAllPastAndEnsureToday();
    }

    // 각 단계는 독립적이다. 정산이 실패해도 오늘 선정 안전망은 계속 실행한다.
    private void settleAllPastAndEnsureToday() {
        LocalDate today = LocalDate.now(clock);

        List<DailyVote> unsettled;
        try {
            unsettled = dailyVoteRepository.findByVoteDateLessThanAndSettledAtIsNullOrderByVoteDateAsc(today);
        } catch (Exception exception) {
            log.error("오늘의 밸런스 미정산 조회 실패. today={}", today, exception);
            eventPublisher.publishEvent(DailyVoteEvent.settleFailed(today, failureDetail(exception)));
            unsettled = List.of();
        }
        for (DailyVote row : unsettled) {
            LocalDate date = row.getVoteDate();
            try {
                settlementService.settle(date)
                        .ifPresent(settled -> eventPublisher.publishEvent(settledEvent(settled)));
            } catch (Exception exception) {
                log.error("오늘의 밸런스 정산 실패. date={}", date, exception);
                eventPublisher.publishEvent(DailyVoteEvent.settleFailed(date, failureDetail(exception)));
            }
        }

        try {
            ensureAndNotify(today);
        } catch (Exception exception) {
            log.error("오늘의 밸런스 선정 실패. date={}", today, exception);
        }
    }

    private String failureDetail(Exception exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }

    // 새로 만들어진 경우에만 SELECTED, 후보가 없으면 NO_CANDIDATE, 이미 예약·선정돼 있으면 알림 없음
    private void ensureAndNotify(LocalDate date) {
        boolean existed = dailyVoteRepository.existsById(date);
        Optional<DailyVote> result = selector.ensureAutoFor(date);
        if (existed) return;
        if (result.isPresent()) {
            DailyVote created = result.get();
            eventPublisher.publishEvent(DailyVoteEvent.selected(
                    date, created.getVoteId(), created.getQuestionSnapshot(), created.getSource().name()));
        } else {
            eventPublisher.publishEvent(DailyVoteEvent.noCandidate(date));
        }
    }

    private DailyVoteEvent settledEvent(DailyVote settled) {
        String detail = "A " + settled.getSettledACount() + "표 · B " + settled.getSettledBCount()
                + "표 · 승자 " + settled.getWinner();
        return DailyVoteEvent.settled(
                settled.getVoteDate(), settled.getVoteId(), settled.getQuestionSnapshot(),
                settled.getSource().name(), detail);
    }
}
