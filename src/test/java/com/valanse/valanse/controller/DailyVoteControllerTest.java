package com.valanse.valanse.controller;

import com.valanse.valanse.domain.DailyVote;
import com.valanse.valanse.domain.DailyVoteParticipation;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.DailyVoteSource;
import com.valanse.valanse.domain.enums.DailyVoteWinner;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.MemberBlock;
import com.valanse.valanse.repository.DailyVoteParticipationRepository;
import com.valanse.valanse.repository.DailyVoteRepository;
import com.valanse.valanse.repository.MemberBlockRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 오늘의 밸런스 공개 조회 API 를 H2 + MockMvc 로 검증합니다. 시계는 2026-11-26 10:00 KST 로 고정합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DailyVoteControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-26T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private DailyVoteRepository dailyVoteRepository;
    @Autowired private DailyVoteParticipationRepository participationRepository;
    @Autowired private MemberBlockRepository memberBlockRepository;

    private Member saveMember() {
        String id = "dvc-" + System.nanoTime();
        return memberRepository.save(Member.builder().socialId(id).email(id + "@example.com").name(id)
                .nickname(id).role(Role.USER).socialType(SocialType.KAKAO).build());
    }

    private Vote saveVote(Member author) {
        Vote vote = Vote.builder().member(author).title("짜장 vs 짬뽕").content("c")
                .category(VoteCategory.ALL).totalVoteCount(0).reactivityScore(0).pinType(PinType.NONE).build();
        vote.addVoteOption(VoteOption.builder().content("짜장").label(VoteLabel.A).voteCount(0).build());
        vote.addVoteOption(VoteOption.builder().content("짬뽕").label(VoteLabel.B).voteCount(0).build());
        return voteRepository.saveAndFlush(vote);
    }

    private DailyVote saveDaily(LocalDate date, Vote vote) {
        DailyVote dv = DailyVote.builder().voteDate(date).build();
        dv.assign(vote, DailyVoteSource.AUTO);
        return dailyVoteRepository.saveAndFlush(dv);
    }

    private DailyVote saveSettled(LocalDate date, int a, int b, DailyVoteWinner winner) {
        DailyVote dv = saveDaily(date, saveVote(saveMember()));
        dv.settle(a, b, winner, LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 15, 0));
        return dailyVoteRepository.saveAndFlush(dv);
    }

    @Test
    @DisplayName("비로그인도_오늘게임을_조회한다")
    void 비로그인도_오늘게임을_조회한다() throws Exception {
        Vote vote = saveVote(saveMember());
        DailyVote dv = saveDaily(TODAY, vote);
        Member voter = saveMember();
        participationRepository.save(DailyVoteParticipation.builder().voteDate(TODAY).voteId(vote.getId())
                .member(voter).firstOptionId(dv.getOptionAId()).currentOptionId(dv.getOptionAId())
                .participationPoint(3).build());

        mockMvc.perform(get("/daily-votes/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-26"))
                .andExpect(jsonPath("$.voteId").value(vote.getId()))
                .andExpect(jsonPath("$.question").value("짜장 vs 짬뽕"))
                .andExpect(jsonPath("$.options[0].label").value("A"))
                .andExpect(jsonPath("$.options[0].id").value(dv.getOptionAId()))
                .andExpect(jsonPath("$.options[0].content").value("짜장"))
                .andExpect(jsonPath("$.options[1].label").value("B"))
                .andExpect(jsonPath("$.todayACount").value(1))
                .andExpect(jsonPath("$.todayBCount").value(0))
                .andExpect(jsonPath("$.authorId").value(vote.getMember().getId()))
                .andExpect(jsonPath("$.authorBlockedByMe").doesNotExist())
                .andExpect(jsonPath("$.my").doesNotExist())
                .andExpect(jsonPath("$.yesterday").doesNotExist());
    }

    @Test
    @DisplayName("오늘게임이_없으면_204")
    void 오늘게임이_없으면_204() throws Exception {
        mockMvc.perform(get("/daily-votes/today")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("endsAt은_다음날_KST_자정")
    void endsAt은_다음날_KST_자정() throws Exception {
        saveDaily(TODAY, saveVote(saveMember()));

        mockMvc.perform(get("/daily-votes/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsAt").value("2026-11-27T00:00:00+09:00"));
    }

    @Test
    @DisplayName("로그인하면_내_참여와_차단여부가_담긴다")
    void 로그인하면_내_참여와_차단여부가_담긴다() throws Exception {
        Member author = saveMember();
        Member me = saveMember();
        Vote vote = saveVote(author);
        DailyVote dv = saveDaily(TODAY, vote);
        participationRepository.save(DailyVoteParticipation.builder().voteDate(TODAY).voteId(vote.getId())
                .member(me).firstOptionId(dv.getOptionAId()).currentOptionId(dv.getOptionBId())
                .participationPoint(3).build());
        memberBlockRepository.save(MemberBlock.builder().blocker(me).blocked(author).build());

        mockMvc.perform(get("/daily-votes/today").with(user(me.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorBlockedByMe").value(true))
                .andExpect(jsonPath("$.my.firstOptionId").value(dv.getOptionAId()))
                .andExpect(jsonPath("$.my.currentOptionId").value(dv.getOptionBId()))
                .andExpect(jsonPath("$.my.participationPoint").value(3));

        Member other = saveMember();
        mockMvc.perform(get("/daily-votes/today").with(user(other.getId().toString())))
                .andExpect(jsonPath("$.authorBlockedByMe").value(false))
                .andExpect(jsonPath("$.my").doesNotExist());
    }

    @Test
    @DisplayName("어제_정산결과가_yesterday에_담긴다")
    void 어제_정산결과가_yesterday에_담긴다() throws Exception {
        Member me = saveMember();
        DailyVote yesterday = saveSettled(TODAY.minusDays(1), 7, 3, DailyVoteWinner.A);
        participationRepository.save(DailyVoteParticipation.builder().voteDate(TODAY.minusDays(1))
                .voteId(yesterday.getVoteId()).member(me).firstOptionId(yesterday.getOptionAId())
                .currentOptionId(yesterday.getOptionAId()).participationPoint(3).bonusPoint(5).build());
        saveDaily(TODAY, saveVote(saveMember()));

        mockMvc.perform(get("/daily-votes/today").with(user(me.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yesterday.date").value("2026-11-25"))
                .andExpect(jsonPath("$.yesterday.aCount").value(7))
                .andExpect(jsonPath("$.yesterday.bCount").value(3))
                .andExpect(jsonPath("$.yesterday.winner").value("A"))
                .andExpect(jsonPath("$.yesterday.optionA").value("짜장"))
                .andExpect(jsonPath("$.yesterday.myBonusPoint").value(5));

        mockMvc.perform(get("/daily-votes/today"))
                .andExpect(jsonPath("$.yesterday.myBonusPoint").doesNotExist());
    }

    @Test
    @DisplayName("목록은_정산된_지난날만_내림차순")
    void 목록은_정산된_지난날만_내림차순() throws Exception {
        saveSettled(TODAY.minusDays(1), 1, 0, DailyVoteWinner.A);
        saveSettled(TODAY.minusDays(2), 2, 2, DailyVoteWinner.TIE);
        saveSettled(TODAY.minusDays(3), 0, 5, DailyVoteWinner.B);
        saveDaily(TODAY.minusDays(4), saveVote(saveMember()));
        saveDaily(TODAY, saveVote(saveMember()));

        mockMvc.perform(get("/daily-votes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].date").value("2026-11-25"))
                .andExpect(jsonPath("$.items[1].date").value("2026-11-24"))
                .andExpect(jsonPath("$.items[2].date").value("2026-11-23"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());

        mockMvc.perform(get("/daily-votes").param("size", "2"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").value("2026-11-24"));

        mockMvc.perform(get("/daily-votes").param("size", "2").param("cursor", "2026-11-24"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].date").value("2026-11-23"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("미래날짜_조회는_404")
    void 미래날짜_조회는_404() throws Exception {
        saveDaily(TODAY.plusDays(1), saveVote(saveMember()));

        mockMvc.perform(get("/daily-votes/2026-11-27")).andExpect(status().isNotFound());
        mockMvc.perform(get("/daily-votes/2026-01-01")).andExpect(status().isNotFound());
        mockMvc.perform(get("/daily-votes/not-a-date")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지난날짜_상세는_요약을_반환한다")
    void 지난날짜_상세는_요약을_반환한다() throws Exception {
        saveSettled(TODAY.minusDays(1), 4, 6, DailyVoteWinner.B);

        mockMvc.perform(get("/daily-votes/2026-11-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winner").value("B"))
                .andExpect(jsonPath("$.aCount").value(4))
                .andExpect(jsonPath("$.acount").doesNotExist());
    }
}
