package com.valanse.valanse.service.DailyVoteService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Report;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.ReportReason;
import com.valanse.valanse.domain.enums.ReportType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.ReportRepository;
import com.valanse.valanse.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 오늘의 밸런스 자동 선정 후보 쿼리와 ensureAutoFor 동작을 H2 로 검증하는 통합 테스트입니다.
 * 시계는 2026-11-26 10:00 KST 로 고정하며, 14일 경계는 2026-11-12 00:00 KST(= 11-11 15:00 UTC)입니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyVoteSelectorIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-26T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired private DailyVoteSelector selector;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private DailyVoteRepository dailyVoteRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("선택지가 3개면 후보가 아니다")
    void 선택지가_3개면_후보가_아니다() {
        Vote threeOptions = saveVote(false, 0, 3);
        Vote twoOptions = saveVote(false, 0, 2);
        setCreatedAtUtc(threeOptions, "2026-11-25 00:00:00");
        setCreatedAtUtc(twoOptions, "2026-11-20 00:00:00");

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(twoOptions.getId());
    }

    @Test
    @DisplayName("신고된 게임은 후보가 아니다")
    void 신고된_게임은_후보가_아니다() {
        Vote reported = saveVote(false, 0, 2);
        Vote clean = saveVote(false, 0, 2);
        setCreatedAtUtc(reported, "2026-11-25 00:00:00");
        setCreatedAtUtc(clean, "2026-11-20 00:00:00");
        reportRepository.saveAndFlush(Report.builder()
                .member(saveMember(false)).reportType(ReportType.VOTE)
                .targetId(reported.getId()).reason(ReportReason.SPAM).build());

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(clean.getId());
    }

    @Test
    @DisplayName("이미 오늘의 게임이었던 게임은 후보가 아니다")
    void 이미_오늘의_게임이었던_게임은_후보가_아니다() {
        Vote used = saveVote(false, 0, 2);
        Vote fresh = saveVote(false, 0, 2);
        setCreatedAtUtc(used, "2026-11-25 00:00:00");
        setCreatedAtUtc(fresh, "2026-11-20 00:00:00");
        DailyVote past = DailyVote.builder().voteDate(TODAY.minusDays(3)).build();
        past.assign(used, DailyVoteSource.AUTO);
        dailyVoteRepository.saveAndFlush(past);

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(fresh.getId());
    }

    @Test
    @DisplayName("삭제된 게임은 후보가 아니다")
    void 삭제된_게임은_후보가_아니다() {
        Vote deleted = saveVote(false, 0, 2);
        Vote alive = saveVote(false, 0, 2);
        setCreatedAtUtc(deleted, "2026-11-25 00:00:00");
        setCreatedAtUtc(alive, "2026-11-20 00:00:00");
        jdbcTemplate.update("update vote set deleted_at = ? where id = ?", LocalDateTime.of(2026, 11, 25, 1, 0), deleted.getId());
        em.clear();

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(alive.getId());
    }

    @Test
    @DisplayName("최근 14일 게임이 있으면 최신순으로, 경계 이전 게임은 제외한다")
    void 최근_14일_게임은_최신순으로_고른다() {
        Vote older = saveVote(false, 999, 2);
        Vote newer = saveVote(false, 0, 2);
        Vote beforeBoundary = saveVote(false, 5000, 2);
        setCreatedAtUtc(older, "2026-11-15 00:00:00");
        setCreatedAtUtc(newer, "2026-11-24 00:00:00");
        // 경계(11-11 15:00 UTC) 직전이라 최근 14일에 들지 않는다.
        setCreatedAtUtc(beforeBoundary, "2026-11-11 14:59:59");

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(newer.getId());
    }

    @Test
    @DisplayName("14일 경계 시각 정각에 생성된 게임은 최근 게임에 포함된다")
    void 경계_정각_게임은_최근_게임이다() {
        Vote onBoundary = saveVote(false, 0, 2);
        Vote highScoreOld = saveVote(false, 5000, 2);
        setCreatedAtUtc(onBoundary, "2026-11-11 15:00:00");
        setCreatedAtUtc(highScoreOld, "2026-11-01 00:00:00");

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(onBoundary.getId());
    }

    @Test
    @DisplayName("최근 14일 게임이 없으면 반응성순으로 고른다")
    void 최근_14일_게임이_없으면_반응성순으로_고른다() {
        Vote low = saveVote(false, 10, 2);
        Vote high = saveVote(false, 500, 2);
        setCreatedAtUtc(low, "2026-10-01 00:00:00");
        setCreatedAtUtc(high, "2026-09-01 00:00:00");

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(high.getId());
    }

    @Test
    @DisplayName("봇이 만든 게임도 후보다")
    void 봇이_만든_게임도_후보다() {
        Vote botVote = saveVote(true, 0, 2);
        setCreatedAtUtc(botVote, "2026-11-25 00:00:00");

        assertThat(selector.pickCandidate()).map(Vote::getId).contains(botVote.getId());
    }

    @Test
    @DisplayName("후보가 없으면 ensureAutoFor 는 비어 있다")
    void 후보가_없으면_ensureAutoFor는_비어있다() {
        assertThat(selector.ensureAutoFor(TODAY.plusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("ensureAutoFor 는 없는 날짜를 AUTO 로 채운다")
    void ensureAutoFor는_없는_날짜를_AUTO로_채운다() {
        Vote vote = saveVote(false, 0, 2);
        setCreatedAtUtc(vote, "2026-11-25 00:00:00");

        Optional<DailyVote> result = selector.ensureAutoFor(TODAY.plusDays(1));

        assertThat(result).isPresent();
        assertThat(result.get().getVoteId()).isEqualTo(vote.getId());
        assertThat(result.get().getSource()).isEqualTo(DailyVoteSource.AUTO);
        assertThat(dailyVoteRepository.findById(TODAY.plusDays(1))).isPresent();
    }

    @Test
    @DisplayName("예약이 있으면 ensureAutoFor 는 덮어쓰지 않는다")
    void 예약이_있으면_ensureAutoFor는_덮어쓰지_않는다() {
        Vote reservedVote = saveVote(false, 0, 2);
        Vote candidate = saveVote(false, 0, 2);
        setCreatedAtUtc(candidate, "2026-11-25 00:00:00");
        DailyVote reserved = DailyVote.builder().voteDate(TODAY.plusDays(1)).build();
        reserved.assign(reservedVote, DailyVoteSource.RESERVED);
        dailyVoteRepository.saveAndFlush(reserved);

        DailyVote result = selector.ensureAutoFor(TODAY.plusDays(1)).orElseThrow();

        assertThat(result.getVoteId()).isEqualTo(reservedVote.getId());
        assertThat(result.getSource()).isEqualTo(DailyVoteSource.RESERVED);
    }

    private Member saveMember(boolean isBot) {
        String socialId = "selector-" + System.nanoTime();
        return memberRepository.save(Member.builder()
                .socialId(socialId)
                .email(socialId + "@example.com")
                .name(socialId)
                .nickname(socialId)
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .isBot(isBot)
                .build());
    }

    private Vote saveVote(boolean botAuthor, int reactivityScore, int optionCount) {
        Vote vote = Vote.builder()
                .member(saveMember(botAuthor))
                .title("vote")
                .content("content")
                .category(VoteCategory.ALL)
                .totalVoteCount(0)
                .reactivityScore(reactivityScore)
                .pinType(PinType.NONE)
                .build();
        VoteLabel[] labels = {VoteLabel.A, VoteLabel.B, VoteLabel.C};
        for (int i = 0; i < optionCount; i++) {
            vote.addVoteOption(VoteOption.builder()
                    .content("opt" + i).label(labels[i]).voteCount(0).build());
        }
        return voteRepository.saveAndFlush(vote);
    }

    /** createdAt 은 감사(Auditing) 필드라 네이티브 쿼리로 UTC 값을 직접 지정합니다. */
    private void setCreatedAtUtc(Vote vote, String utc) {
        jdbcTemplate.update("update vote set created_at = ? where id = ?",
                java.sql.Timestamp.valueOf(utc), vote.getId());
        em.clear();
    }
}
