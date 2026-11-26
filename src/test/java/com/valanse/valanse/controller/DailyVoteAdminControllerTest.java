package com.valanse.valanse.controller;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 오늘의 밸런스 관리자 API 를 H2 + MockMvc 로 검증합니다. 시계는 2026-11-26 10:00 KST 로 고정합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DailyVoteAdminControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 26);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

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

    private Member saveMember() {
        String id = "dva-" + System.nanoTime();
        return memberRepository.save(Member.builder().socialId(id).email(id + "@example.com").name(id)
                .nickname(id).role(Role.USER).socialType(SocialType.KAKAO).build());
    }

    private Vote saveVote(String title, int optionCount) {
        Vote vote = Vote.builder().member(saveMember()).title(title).content("c")
                .category(VoteCategory.ALL).totalVoteCount(0).reactivityScore(0).pinType(PinType.NONE).build();
        VoteLabel[] labels = VoteLabel.values();
        for (int i = 0; i < optionCount; i++) {
            vote.addVoteOption(VoteOption.builder().content("옵션" + i).label(labels[i]).voteCount(0).build());
        }
        return voteRepository.saveAndFlush(vote);
    }

    private String body(Long voteId) {
        return "{\"voteId\":" + voteId + "}";
    }

    @Test
    @DisplayName("관리자가_아니면_403")
    @WithMockUser(roles = "USER")
    void 관리자가_아니면_403() throws Exception {
        mockMvc.perform(get("/admin/daily-votes").param("from", "2026-11-20").param("to", "2026-11-30"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/admin/daily-votes/2026-11-27").contentType(MediaType.APPLICATION_JSON).content(body(1L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/admin/daily-votes/2026-11-27"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("오늘_날짜는_예약할_수_없다")
    @WithMockUser(roles = "ADMIN")
    void 오늘_날짜는_예약할_수_없다() throws Exception {
        Vote vote = saveVote("q", 2);

        mockMvc.perform(put("/admin/daily-votes/2026-11-26").contentType(MediaType.APPLICATION_JSON).content(body(vote.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("오늘 및 지난 날짜는 변경할 수 없습니다. 내일 이후만 가능합니다."));
        mockMvc.perform(delete("/admin/daily-votes/2026-11-26"))
                .andExpect(status().isBadRequest());
        assertThat(dailyVoteRepository.findById(TODAY)).isEmpty();
    }

    @Test
    @DisplayName("선택지_3개_게임은_예약할_수_없다")
    @WithMockUser(roles = "ADMIN")
    void 선택지_3개_게임은_예약할_수_없다() throws Exception {
        Vote vote = saveVote("q", 3);

        mockMvc.perform(put("/admin/daily-votes/2026-11-27").contentType(MediaType.APPLICATION_JSON).content(body(vote.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("선택지가 정확히 2개(A, B)인 게임만 예약할 수 있습니다."));
        assertThat(dailyVoteRepository.findById(TOMORROW)).isEmpty();
    }

    @Test
    @DisplayName("없는_게임은_404")
    @WithMockUser(roles = "ADMIN")
    void 없는_게임은_404() throws Exception {
        mockMvc.perform(put("/admin/daily-votes/2026-11-27").contentType(MediaType.APPLICATION_JSON).content(body(999999999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("게임을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("예약하면_RESERVED로_저장되고_스냅샷이_채워진다")
    @WithMockUser(roles = "ADMIN")
    void 예약하면_RESERVED로_저장되고_스냅샷이_채워진다() throws Exception {
        Vote vote = saveVote("짜장 vs 짬뽕", 2);

        mockMvc.perform(put("/admin/daily-votes/2026-11-27").contentType(MediaType.APPLICATION_JSON).content(body(vote.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-27"))
                .andExpect(jsonPath("$.voteId").value(vote.getId()))
                .andExpect(jsonPath("$.question").value("짜장 vs 짬뽕"))
                .andExpect(jsonPath("$.source").value("RESERVED"))
                .andExpect(jsonPath("$.settled").value(false));

        DailyVote saved = dailyVoteRepository.findById(TOMORROW).orElseThrow();
        assertThat(saved.getSource()).isEqualTo(DailyVoteSource.RESERVED);
        assertThat(saved.getQuestionSnapshot()).isEqualTo("짜장 vs 짬뽕");
        assertThat(saved.getOptionASnapshot()).isEqualTo("옵션0");
        assertThat(saved.getOptionBSnapshot()).isEqualTo("옵션1");
        assertThat(saved.getOptionAId()).isNotNull();
        assertThat(saved.getOptionBId()).isNotNull();
    }

    @Test
    @DisplayName("자동선정된_내일을_예약으로_바꿀수있다")
    @WithMockUser(roles = "ADMIN")
    void 자동선정된_내일을_예약으로_바꿀수있다() throws Exception {
        Vote auto = saveVote("자동", 2);
        Vote manual = saveVote("수동", 2);
        DailyVote dv = DailyVote.builder().voteDate(TOMORROW).build();
        dv.assign(auto, DailyVoteSource.AUTO);
        dailyVoteRepository.saveAndFlush(dv);

        mockMvc.perform(put("/admin/daily-votes/2026-11-27").contentType(MediaType.APPLICATION_JSON).content(body(manual.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteId").value(manual.getId()))
                .andExpect(jsonPath("$.source").value("RESERVED"));

        DailyVote saved = dailyVoteRepository.findById(TOMORROW).orElseThrow();
        assertThat(saved.getVoteId()).isEqualTo(manual.getId());
        assertThat(saved.getQuestionSnapshot()).isEqualTo("수동");
        assertThat(saved.getSource()).isEqualTo(DailyVoteSource.RESERVED);
    }

    @Test
    @DisplayName("미래_예약을_취소한다")
    @WithMockUser(roles = "ADMIN")
    void 미래_예약을_취소한다() throws Exception {
        DailyVote dv = DailyVote.builder().voteDate(TOMORROW).build();
        dv.assign(saveVote("q", 2), DailyVoteSource.RESERVED);
        dailyVoteRepository.saveAndFlush(dv);

        mockMvc.perform(delete("/admin/daily-votes/2026-11-27")).andExpect(status().isNoContent());
        assertThat(dailyVoteRepository.findById(TOMORROW)).isEmpty();

        mockMvc.perform(delete("/admin/daily-votes/2026-11-27"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("해당 날짜에 예약된 게임이 없습니다."));
    }

    @Test
    @DisplayName("기간_조회는_행이_있는_날만_정확한_키로_반환한다")
    @WithMockUser(roles = "ADMIN")
    void 기간_조회는_행이_있는_날만_정확한_키로_반환한다() throws Exception {
        DailyVote settled = DailyVote.builder().voteDate(TODAY.minusDays(1)).build();
        settled.assign(saveVote("어제", 2), DailyVoteSource.AUTO);
        settled.settle(7, 3, com.valanse.valanse.domain.enums.DailyVoteWinner.A, java.time.LocalDateTime.of(2026, 11, 25, 15, 0));
        dailyVoteRepository.saveAndFlush(settled);
        DailyVote future = DailyVote.builder().voteDate(TOMORROW).build();
        future.assign(saveVote("내일", 2), DailyVoteSource.RESERVED);
        dailyVoteRepository.saveAndFlush(future);

        mockMvc.perform(get("/admin/daily-votes").param("from", "2026-11-24").param("to", "2026-11-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-11-25"))
                .andExpect(jsonPath("$[0].source").value("AUTO"))
                .andExpect(jsonPath("$[0].settled").value(true))
                .andExpect(jsonPath("$[0].aCount").value(7))
                .andExpect(jsonPath("$[0].bCount").value(3))
                .andExpect(jsonPath("$[0].acount").doesNotExist())
                .andExpect(jsonPath("$[1].date").value("2026-11-27"))
                .andExpect(jsonPath("$[1].settled").value(false))
                .andExpect(jsonPath("$[1].aCount").doesNotExist());
    }

    @Test
    @DisplayName("기간이_잘못되면_400")
    @WithMockUser(roles = "ADMIN")
    void 기간이_잘못되면_400() throws Exception {
        mockMvc.perform(get("/admin/daily-votes").param("from", "2026-11-30").param("to", "2026-11-20"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/daily-votes").param("from", "2026-01-01").param("to", "2026-03-05"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/daily-votes").param("from", "2026-01-01").param("to", "2026-03-03"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/daily-votes").param("from", "2026-01-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/admin/daily-votes").param("from", "x").param("to", "2026-01-02"))
                .andExpect(status().isBadRequest());
    }
}
