package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.common.alert.DailyVoteEvent;
import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 오늘의 게임 삭제 시 교체·예약 해제·과거 보존 동작을 H2 로 검증하는 통합 테스트입니다.
 * 시계는 2026-11-26 10:00 KST 로 고정합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RecordApplicationEvents
class DailyVoteLifecycleServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-26T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired private DailyVoteLifecycleService lifecycleService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private DailyVoteRepository dailyVoteRepository;
    @Autowired private ApplicationEvents events;

    @Test
    @DisplayName("오늘 게임을 삭제하면 즉시 교체된다")
    void 오늘_게임을_삭제하면_즉시_교체된다() {
        Vote today = saveVote("오늘 게임");
        Vote replacement = saveVote("교체될 게임");
        saveDaily(TODAY, today, DailyVoteSource.RESERVED);

        // flush 없이 soft delete 후 호출 → 후보 JPQL 의 auto flush 로 삭제 게임이 제외되는지까지 확인
        today.softDelete();
        voteRepository.save(today);
        lifecycleService.onVoteDeleted(today.getId());

        DailyVote row = dailyVoteRepository.findById(TODAY).orElseThrow();
        assertThat(row.getVoteId()).isEqualTo(replacement.getId());
        assertThat(row.getQuestionSnapshot()).isEqualTo("교체될 게임");
        assertThat(row.getSource()).isEqualTo(DailyVoteSource.AUTO);
        assertThat(events.stream(DailyVoteEvent.class))
                .extracting(DailyVoteEvent::type).containsExactly(DailyVoteEvent.Type.REPLACED);
    }

    @Test
    @DisplayName("후보가 없으면 오늘 행을 그대로 두고 알림만 보낸다")
    void 후보가_없으면_오늘_행을_그대로_둔다() {
        Vote today = saveVote("오늘 게임");
        saveDaily(TODAY, today, DailyVoteSource.AUTO);

        today.softDelete();
        voteRepository.save(today);
        lifecycleService.onVoteDeleted(today.getId());

        DailyVote row = dailyVoteRepository.findById(TODAY).orElseThrow();
        assertThat(row.getVoteId()).isEqualTo(today.getId());
        assertThat(row.getQuestionSnapshot()).isEqualTo("오늘 게임");
        assertThat(events.stream(DailyVoteEvent.class))
                .extracting(DailyVoteEvent::type).containsExactly(DailyVoteEvent.Type.NO_CANDIDATE);
    }

    @Test
    @DisplayName("내일 예약된 게임을 삭제하면 예약이 풀린다")
    void 내일_예약된_게임을_삭제하면_예약이_풀린다() {
        Vote reserved = saveVote("예약 게임");
        saveDaily(TODAY.plusDays(1), reserved, DailyVoteSource.RESERVED);

        reserved.softDelete();
        voteRepository.save(reserved);
        lifecycleService.onVoteDeleted(reserved.getId());

        assertThat(dailyVoteRepository.findById(TODAY.plusDays(1))).isEmpty();
        assertThat(events.stream(DailyVoteEvent.class))
                .extracting(DailyVoteEvent::type).containsExactly(DailyVoteEvent.Type.RESERVATION_CANCELLED);
    }

    @Test
    @DisplayName("지난 날짜는 삭제해도 그대로다")
    void 지난_날짜는_삭제해도_그대로다() {
        Vote past = saveVote("지난 게임");
        saveDaily(TODAY.minusDays(2), past, DailyVoteSource.AUTO);

        past.softDelete();
        voteRepository.save(past);
        lifecycleService.onVoteDeleted(past.getId());

        DailyVote row = dailyVoteRepository.findById(TODAY.minusDays(2)).orElseThrow();
        assertThat(row.getVoteId()).isEqualTo(past.getId());
        assertThat(row.getQuestionSnapshot()).isEqualTo("지난 게임");
        assertThat(events.stream(DailyVoteEvent.class)).isEmpty();
    }

    private void saveDaily(LocalDate date, Vote vote, DailyVoteSource source) {
        DailyVote row = DailyVote.builder().voteDate(date).build();
        row.assign(vote, source);
        dailyVoteRepository.saveAndFlush(row);
    }

    private Vote saveVote(String title) {
        Member member = memberRepository.saveAndFlush(Member.builder()
                .socialId("social-" + System.nanoTime()).socialType(SocialType.KAKAO).role(Role.USER)
                .email("t@example.com").name("테스터").build());
        Vote vote = Vote.builder()
                .member(member).title(title).content("content").category(VoteCategory.ALL)
                .totalVoteCount(0).reactivityScore(0).pinType(PinType.NONE).build();
        vote.addVoteOption(VoteOption.builder().content("A").label(VoteLabel.A).voteCount(0).build());
        vote.addVoteOption(VoteOption.builder().content("B").label(VoteLabel.B).voteCount(0).build());
        return voteRepository.saveAndFlush(vote);
    }
}
