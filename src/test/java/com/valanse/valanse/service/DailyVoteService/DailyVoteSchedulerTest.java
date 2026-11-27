package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.common.alert.DailyVoteEvent;
import com.valanse.valanse.common.config.DailyVoteProperties;
import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.domain.enums.DailyVoteWinner;
import com.valanse.valanse.repository.DailyVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyVoteSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Mock private DailyVoteSelector selector;
    @Mock private DailyVoteSettlementService settlementService;
    @Mock private DailyVoteRepository dailyVoteRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DailyVoteProperties properties;
    private DailyVoteScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new DailyVoteProperties();
        // KST 2026-11-26 10:00
        Clock clock = Clock.fixed(Instant.parse("2026-11-26T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        scheduler = new DailyVoteScheduler(
                selector, settlementService, dailyVoteRepository, properties, eventPublisher, clock);
    }

    @Test
    @DisplayName("enabled가 false면 아무것도 하지 않는다")
    void enabled가_false면_아무것도_안한다() {
        properties.setEnabled(false);

        scheduler.selectTomorrow();
        scheduler.settleAndEnsureToday();
        scheduler.hourlySafetyNet();

        verifyNoInteractions(selector, settlementService, dailyVoteRepository, eventPublisher);
    }

    @Test
    @DisplayName("내일 예약이 이미 있으면 알림을 보내지 않는다")
    void 내일_예약이_있으면_알림을_보내지_않는다() {
        when(dailyVoteRepository.existsById(TOMORROW)).thenReturn(true);
        when(selector.ensureAutoFor(TOMORROW)).thenReturn(Optional.of(dailyVote(TOMORROW, DailyVoteSource.RESERVED)));

        scheduler.selectTomorrow();

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("내일 AUTO로 새로 선정되면 SELECTED를 발행한다")
    void 새로_선정되면_SELECTED를_발행한다() {
        when(dailyVoteRepository.existsById(TOMORROW)).thenReturn(false);
        when(selector.ensureAutoFor(TOMORROW)).thenReturn(Optional.of(dailyVote(TOMORROW, DailyVoteSource.AUTO)));

        scheduler.selectTomorrow();

        DailyVoteEvent event = captureSingleEvent();
        assertThat(event.type()).isEqualTo(DailyVoteEvent.Type.SELECTED);
        assertThat(event.date()).isEqualTo(TOMORROW);
        assertThat(event.voteId()).isEqualTo(10L);
        assertThat(event.question()).isEqualTo("질문");
        assertThat(event.source()).isEqualTo("AUTO");
    }

    @Test
    @DisplayName("후보가 없으면 NO_CANDIDATE를 발행한다")
    void 후보가_없으면_NO_CANDIDATE를_발행한다() {
        when(dailyVoteRepository.existsById(TOMORROW)).thenReturn(false);
        when(selector.ensureAutoFor(TOMORROW)).thenReturn(Optional.empty());

        scheduler.selectTomorrow();

        DailyVoteEvent event = captureSingleEvent();
        assertThat(event.type()).isEqualTo(DailyVoteEvent.Type.NO_CANDIDATE);
        assertThat(event.date()).isEqualTo(TOMORROW);
    }

    @Test
    @DisplayName("자정에는 어제를 정산하고 오늘이 비면 채운다")
    void 자정에는_어제를_정산하고_오늘이_비면_채운다() {
        DailyVote settled = dailyVote(YESTERDAY, DailyVoteSource.AUTO);
        settled.settle(7, 3, DailyVoteWinner.A, LocalDateTime.of(2026, 11, 25, 15, 0));
        when(dailyVoteRepository.findByVoteDateLessThanAndSettledAtIsNullOrderByVoteDateAsc(TODAY))
                .thenReturn(List.of(dailyVote(YESTERDAY, DailyVoteSource.AUTO)));
        when(settlementService.settle(YESTERDAY)).thenReturn(Optional.of(settled));
        when(dailyVoteRepository.existsById(TODAY)).thenReturn(false);
        when(selector.ensureAutoFor(TODAY)).thenReturn(Optional.of(dailyVote(TODAY, DailyVoteSource.AUTO)));

        scheduler.settleAndEnsureToday();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(captor.capture());
        List<DailyVoteEvent> events = captor.getAllValues().stream().map(DailyVoteEvent.class::cast).toList();
        assertThat(events).extracting(DailyVoteEvent::type)
                .containsExactly(DailyVoteEvent.Type.SETTLED, DailyVoteEvent.Type.SELECTED);
        assertThat(events.get(0).date()).isEqualTo(YESTERDAY);
        assertThat(events.get(0).detail()).contains("A 7표").contains("B 3표").contains("A");
        assertThat(events.get(1).date()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("정산이 실패해도 오늘 선정은 계속 실행한다")
    void 정산이_실패해도_오늘_선정은_계속한다() {
        when(dailyVoteRepository.findByVoteDateLessThanAndSettledAtIsNullOrderByVoteDateAsc(TODAY))
                .thenReturn(List.of(dailyVote(YESTERDAY, DailyVoteSource.AUTO)));
        when(settlementService.settle(YESTERDAY)).thenThrow(new IllegalStateException("boom"));
        when(dailyVoteRepository.existsById(TODAY)).thenReturn(false);
        when(selector.ensureAutoFor(TODAY)).thenReturn(Optional.empty());

        scheduler.settleAndEnsureToday();

        List<DailyVoteEvent> events = captureEvents(2);
        assertThat(events).extracting(DailyVoteEvent::type)
                .containsExactly(DailyVoteEvent.Type.SETTLE_FAILED, DailyVoteEvent.Type.NO_CANDIDATE);
    }

    @Test
    @DisplayName("이미 정산됐고 오늘 예약이 있으면 알림이 없다")
    void 이미_정산됐고_오늘_예약이_있으면_알림이_없다() {
        when(dailyVoteRepository.existsById(TODAY)).thenReturn(true);
        when(selector.ensureAutoFor(TODAY)).thenReturn(Optional.of(dailyVote(TODAY, DailyVoteSource.RESERVED)));

        scheduler.settleAndEnsureToday();

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("매시 안전망은 미정산 지난 날을 모두 정산한다")
    void 매시_안전망은_미정산_지난날을_모두_정산한다() {
        LocalDate twoDaysAgo = TODAY.minusDays(2);
        when(dailyVoteRepository.findByVoteDateLessThanAndSettledAtIsNullOrderByVoteDateAsc(TODAY))
                .thenReturn(List.of(dailyVote(twoDaysAgo, DailyVoteSource.AUTO), dailyVote(YESTERDAY, DailyVoteSource.AUTO)));
        when(settlementService.settle(twoDaysAgo)).thenReturn(Optional.of(settledRow(twoDaysAgo)));
        when(settlementService.settle(YESTERDAY)).thenReturn(Optional.of(settledRow(YESTERDAY)));
        when(dailyVoteRepository.existsById(TODAY)).thenReturn(true);
        when(selector.ensureAutoFor(TODAY)).thenReturn(Optional.of(dailyVote(TODAY, DailyVoteSource.AUTO)));

        scheduler.hourlySafetyNet();

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(settlementService);
        inOrder.verify(settlementService).settle(twoDaysAgo);
        inOrder.verify(settlementService).settle(YESTERDAY);
        List<DailyVoteEvent> events = captureEvents(2);
        assertThat(events).extracting(DailyVoteEvent::type)
                .containsExactly(DailyVoteEvent.Type.SETTLED, DailyVoteEvent.Type.SETTLED);
        assertThat(events).extracting(DailyVoteEvent::date).containsExactly(twoDaysAgo, YESTERDAY);
    }

    @Test
    @DisplayName("정산 하나가 실패해도 나머지는 정산하고 SETTLE_FAILED를 보낸다")
    void 정산_하나가_실패해도_나머지는_정산하고_SETTLE_FAILED를_보낸다() {
        LocalDate twoDaysAgo = TODAY.minusDays(2);
        when(dailyVoteRepository.findByVoteDateLessThanAndSettledAtIsNullOrderByVoteDateAsc(TODAY))
                .thenReturn(List.of(dailyVote(twoDaysAgo, DailyVoteSource.AUTO), dailyVote(YESTERDAY, DailyVoteSource.AUTO)));
        when(settlementService.settle(twoDaysAgo)).thenThrow(new IllegalStateException("boom"));
        when(settlementService.settle(YESTERDAY)).thenReturn(Optional.of(settledRow(YESTERDAY)));
        when(dailyVoteRepository.existsById(TODAY)).thenReturn(true);
        when(selector.ensureAutoFor(TODAY)).thenReturn(Optional.of(dailyVote(TODAY, DailyVoteSource.AUTO)));

        scheduler.hourlySafetyNet();

        List<DailyVoteEvent> events = captureEvents(2);
        assertThat(events).extracting(DailyVoteEvent::type)
                .containsExactly(DailyVoteEvent.Type.SETTLE_FAILED, DailyVoteEvent.Type.SETTLED);
        assertThat(events.get(0).date()).isEqualTo(twoDaysAgo);
        assertThat(events.get(0).detail()).contains("IllegalStateException").contains("boom");
    }

    @Test
    @DisplayName("매시 안전망은 오늘이 비면 채운다")
    void 매시_안전망은_오늘이_비면_채운다() {
        when(dailyVoteRepository.existsById(TODAY)).thenReturn(false);
        when(selector.ensureAutoFor(TODAY)).thenReturn(Optional.of(dailyVote(TODAY, DailyVoteSource.AUTO)));

        scheduler.hourlySafetyNet();

        DailyVoteEvent event = captureSingleEvent();
        assertThat(event.type()).isEqualTo(DailyVoteEvent.Type.SELECTED);
        assertThat(event.date()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("enabled가 false면 안전망도 아무것도 안한다")
    void enabled가_false면_안전망도_아무것도_안한다() {
        properties.setEnabled(false);

        scheduler.hourlySafetyNet();

        verifyNoInteractions(selector, settlementService, dailyVoteRepository, eventPublisher);
    }

    private DailyVote settledRow(LocalDate date) {
        DailyVote row = dailyVote(date, DailyVoteSource.AUTO);
        row.settle(1, 0, DailyVoteWinner.A, LocalDateTime.of(2026, 11, 25, 15, 0));
        return row;
    }

    private List<DailyVoteEvent> captureEvents(int times) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(times)).publishEvent(captor.capture());
        return captor.getAllValues().stream().map(DailyVoteEvent.class::cast).toList();
    }

    private DailyVoteEvent captureSingleEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return (DailyVoteEvent) captor.getValue();
    }

    private DailyVote dailyVote(LocalDate date, DailyVoteSource source) {
        return DailyVote.builder()
                .voteDate(date).voteId(10L).source(source).questionSnapshot("질문").build();
    }
}
