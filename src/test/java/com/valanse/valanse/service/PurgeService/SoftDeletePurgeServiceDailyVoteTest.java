package com.valanse.valanse.service.PurgeService;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.DailyVoteParticipation;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.repository.DailyVoteParticipationRepository;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.service.StorageService.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게임을 하드 삭제(purge)해도 daily_vote 스냅샷과 참여 기록이 남고 예외가 없음을 H2 로 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SoftDeletePurgeServiceDailyVoteTest {

    @Autowired private SoftDeletePurgeService purgeService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private DailyVoteRepository dailyVoteRepository;
    @Autowired private DailyVoteParticipationRepository participationRepository;
    @MockBean private StorageService storageService;

    @Test
    @DisplayName("퍼지해도 daily_vote 스냅샷은 남는다")
    void 퍼지해도_daily_vote_스냅샷은_남는다() {
        Member member = memberRepository.saveAndFlush(Member.builder()
                .socialId("social-" + System.nanoTime()).email("test@example.com").name("테스터").build());
        Vote vote = Vote.builder().member(member).category(VoteCategory.ETC).title("스냅샷 질문").content("본문").build();
        vote.addVoteOption(VoteOption.builder().content("A선택").label(VoteLabel.A).build());
        vote.addVoteOption(VoteOption.builder().content("B선택").label(VoteLabel.B).build());
        vote = voteRepository.saveAndFlush(vote);
        Long voteId = vote.getId();
        Long optionAId = vote.getVoteOptions().get(0).getId();

        LocalDate date = LocalDate.of(2026, 11, 1);
        DailyVote daily = DailyVote.builder().voteDate(date).build();
        daily.assign(vote, DailyVoteSource.AUTO);
        dailyVoteRepository.saveAndFlush(daily);
        participationRepository.saveAndFlush(DailyVoteParticipation.builder()
                .voteDate(date).voteId(voteId).member(member)
                .firstOptionId(optionAId).currentOptionId(optionAId).participationPoint(3).build());
        jdbcTemplate.update("update vote set deleted_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(15)), voteId);

        PurgeResult result = purgeService.purgeExpired(LocalDateTime.now());

        assertThat(result.votesDeleted()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from vote where id = ?", Integer.class, voteId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select vote_id from daily_vote where vote_date = ?", Long.class, date)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "select question_snapshot from daily_vote where vote_date = ?", String.class, date)).isEqualTo("스냅샷 질문");
        assertThat(jdbcTemplate.queryForObject(
                "select option_a_snapshot from daily_vote where vote_date = ?", String.class, date)).isEqualTo("A선택");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from daily_vote_participation where vote_date = ? and vote_id is null and first_option_id is null and current_option_id is null",
                Integer.class, date)).isEqualTo(1);
    }
}
