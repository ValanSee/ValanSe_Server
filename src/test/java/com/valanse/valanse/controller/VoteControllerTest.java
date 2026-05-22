// src/test/java/com/valanse/valanse/controller/VoteControllerTest.java
package com.valanse.valanse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.*;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class VoteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VoteRepository voteRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        voteRepository.deleteAll();
        commentGroupRepository.deleteAll();
        memberProfileRepository.deleteAll();
        memberRepository.deleteAll();

        Member member1 = Member.builder()
                .socialId("kakao123").email("test1@example.com").name("테스터1")
                .profile_image_url("http://image.com/test1.jpg")
                .kakaoAccessToken("token1").kakaoRefreshToken("refresh1").build();
        memberRepository.save(member1);

        memberProfileRepository.save(MemberProfile.builder()
                .member(member1).nickname("테스터1닉네임")
                .gender(Gender.MALE).age(Age.TWENTY).mbti("ENFP").build());

        Vote hotIssueVote = Vote.builder()
                .category(VoteCategory.FOOD).title("오늘의 점심 선택은?")
                .totalVoteCount(100).reactivityScore(110)
                .reactivityUpdatedAt(LocalDateTime.now())
                .member(member1).pinType(PinType.NONE).build();
        voteRepository.save(hotIssueVote);

        commentGroupRepository.save(CommentGroup.builder()
                .vote(hotIssueVote).totalCommentCount(10).build());

        VoteOption optionA = VoteOption.builder().vote(hotIssueVote)
                .content("A. 맵고 얼큰한 라면").voteCount(60).label(VoteLabel.A).build();
        VoteOption optionB = VoteOption.builder().vote(hotIssueVote)
                .content("B. 부드러운 파스타").voteCount(40).label(VoteLabel.B).build();
        hotIssueVote.getVoteOptions().addAll(Arrays.asList(optionA, optionB));
        voteRepository.save(hotIssueVote);

        Member member2 = Member.builder()
                .socialId("kakao456").email("test2@example.com").name("테스터2")
                .profile_image_url("http://image.com/test2.jpg")
                .kakaoAccessToken("token2").kakaoRefreshToken("refresh2").build();
        memberRepository.save(member2);

        memberProfileRepository.save(MemberProfile.builder()
                .member(member2).nickname("테스터2닉네임")
                .gender(Gender.FEMALE).age(Age.THIRTY).mbti("ISTJ").build());

        Vote otherVote = Vote.builder()
                .category(VoteCategory.LOVE).title("연애 밸런스 게임")
                .totalVoteCount(50).reactivityScore(55)
                .reactivityUpdatedAt(LocalDateTime.now())
                .member(member2).pinType(PinType.NONE).build();
        voteRepository.save(otherVote);

        commentGroupRepository.save(CommentGroup.builder()
                .vote(otherVote).totalCommentCount(5).build());
    }

    @Test
    @DisplayName("반응성이 가장 높은 핫이슈 투표 정보를 성공적으로 조회한다.")
    void getHotIssueVote_Success() throws Exception {
        mockMvc.perform(get("/votes/best").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteId").isNumber())
                .andExpect(jsonPath("$.title").value("오늘의 점심 선택은?"))
                .andExpect(jsonPath("$.category").value(VoteCategory.FOOD.name()))
                .andExpect(jsonPath("$.totalParticipants").value(100))
                .andExpect(jsonPath("$.createdBy").value("테스터1닉네임"))
                .andExpect(jsonPath("$.options[0].content").value("A. 맵고 얼큰한 라면"))
                .andExpect(jsonPath("$.options[0].vote_count").value(60))
                .andExpect(jsonPath("$.options[1].content").value("B. 부드러운 파스타"))
                .andExpect(jsonPath("$.options[1].vote_count").value(40));
    }

    @Test
    @DisplayName("핫이슈 투표가 없을 때 404 Not Found를 반환한다.")
    void getHotIssueVote_NotFound() throws Exception {
        commentGroupRepository.deleteAll();
        voteRepository.deleteAll();

        mockMvc.perform(get("/votes/best").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("핫이슈 투표를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.status").value(404));
        // 참고: GlobalExceptionHandler에서 "type" 필드는 주석 처리되어 있음
    }

    @Test
    @DisplayName("동일한 반응성을 가진 투표 중 최신 투표를 조회한다.")
    void getHotIssueVote_SameTotalVoteCount_NewerIsHotIssue() throws Exception {
        commentGroupRepository.deleteAll();
        voteRepository.deleteAll();

        Member member3 = Member.builder()
                .socialId("kakao789").email("test3@example.com").name("테스터3")
                .profile_image_url("http://image.com/test3.jpg")
                .kakaoAccessToken("token3").kakaoRefreshToken("refresh3").build();
        memberRepository.save(member3);

        memberProfileRepository.save(MemberProfile.builder()
                .member(member3).nickname("테스터3닉네임")
                .gender(Gender.MALE).age(Age.OVER_FORTY).mbti("INTP").build());

        Vote oldVote = Vote.builder()
                .category(VoteCategory.ETC).title("오래된 핫이슈 투표")
                .totalVoteCount(50).reactivityScore(50)
                .reactivityUpdatedAt(LocalDateTime.now().minusDays(3))
                .member(member3).pinType(PinType.NONE).build();
        voteRepository.save(oldVote);
        commentGroupRepository.save(CommentGroup.builder().vote(oldVote).totalCommentCount(0).build());

        Vote newVote = Vote.builder()
                .category(VoteCategory.LOVE).title("새로운 핫이슈 투표")
                .totalVoteCount(50).reactivityScore(50)
                .reactivityUpdatedAt(LocalDateTime.now())
                .member(member3).pinType(PinType.NONE).build();
        voteRepository.save(newVote);
        commentGroupRepository.save(CommentGroup.builder().vote(newVote).totalCommentCount(0).build());

        mockMvc.perform(get("/votes/best").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("새로운 핫이슈 투표"))
                .andExpect(jsonPath("$.totalParticipants").value(50))
                .andExpect(jsonPath("$.createdBy").value("테스터3닉네임"));
    }

    @Test
    @DisplayName("투표 목록 조회 시 목록 데이터와 연관 데이터를 고정된 쿼리 수로 조회한다.")
    void getVotes_QueryCountIsStable() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        mockMvc.perform(get("/votes")
                        .param("category", "ALL")
                        .param("sort", "latest")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(2))
                .andExpect(jsonPath("$.votes[0].nickname").value("테스터2닉네임"))
                .andExpect(jsonPath("$.votes[1].nickname").value("테스터1닉네임"))
                .andExpect(jsonPath("$.votes[1].options.length()").value(2))
                .andExpect(jsonPath("$.has_next_page").value(false));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("투표 목록 조회 시 size가 1보다 작으면 400 Bad Request를 반환한다.")
    void getVotes_InvalidSize_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("size는 1 이상이어야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("투표 목록 조회 시 잘못된 category는 400 Bad Request를 반환한다.")
    void getVotes_InvalidCategory_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("category", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("category는 ALL, FOOD, LOVE, ETC 중 하나여야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("투표 목록 조회 시 잘못된 sort는 400 Bad Request를 반환한다.")
    void getVotes_InvalidSort_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("sort", "oldest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("sort는 latest 또는 popular 중 하나여야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("투표 목록 조회 시 latest cursor 형식이 잘못되면 400 Bad Request를 반환한다.")
    void getVotes_InvalidLatestCursor_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("sort", "latest")
                        .param("cursor", "2026-05-18T13:00:00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("cursor 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("투표 목록 조회 시 popular cursor 형식이 잘못되면 400 Bad Request를 반환한다.")
    void getVotes_InvalidPopularCursor_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("sort", "popular")
                        .param("cursor", "100_2026-05-18T13:00:00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("cursor 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.status").value(400));
    }
}
