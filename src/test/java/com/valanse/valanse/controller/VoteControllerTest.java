// src/test/java/com/valanse/valanse/controller/VoteControllerTest.java
package com.valanse.valanse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valanse.valanse.domain.*;
import com.valanse.valanse.domain.enums.*;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private Long member1Id;
    private Long hotIssueVoteId;

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
        member1Id = member1.getId();

        memberProfileRepository.save(MemberProfile.builder()
                .member(member1).nickname("테스터1닉네임")
                .gender(Gender.MALE).age(Age.TWENTY).mbti("ENFP").build());

        Vote hotIssueVote = Vote.builder()
                .category(VoteCategory.FOOD).title("오늘의 점심 선택은?")
                .totalVoteCount(100).reactivityScore(110)
                .reactivityUpdatedAt(LocalDateTime.now())
                .member(member1).pinType(PinType.NONE).build();
        voteRepository.save(hotIssueVote);
        hotIssueVoteId = hotIssueVote.getId();

        commentGroupRepository.save(CommentGroup.builder()
                .vote(hotIssueVote).totalCommentCount(10).build());

        VoteOption optionA = VoteOption.builder().vote(hotIssueVote)
                .content("A. 맵고 얼큰한 라면").voteCount(60).label(VoteLabel.A).build();
        VoteOption optionB = VoteOption.builder().vote(hotIssueVote)
                .content("B. 부드러운 파스타").voteCount(40).label(VoteLabel.B).build();
        hotIssueVote.getVoteOptions().addAll(Arrays.asList(optionA, optionB));
        voteOptionRepository.saveAll(Arrays.asList(optionA, optionB));

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

        memberVoteOptionRepository.save(MemberVoteOption.builder()
                .member(member2)
                .vote(hotIssueVote)
                .voteOption(optionA)
                .build());
    }

    @Test
    @DisplayName("비로그인 사용자는 자신이 만든 투표 목록을 조회할 수 없다.")
    void getMyCreatedVotes_Anonymous_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/votes/mine/created"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비로그인 사용자는 자신이 참여한 투표 목록을 조회할 수 없다.")
    void getMyVotedVotes_Anonymous_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/votes/mine/voted"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 사용자는 자신이 만든 투표 목록을 조회할 수 있다.")
    void getMyCreatedVotes_Authenticated_ReturnsVotes() throws Exception {
        mockMvc.perform(get("/votes/mine/created")
                        .with(user(member1Id.toString()))
                        .param("category", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].voteId").value(hotIssueVoteId));
    }

    @Test
    @DisplayName("로그인 사용자는 자신이 참여한 투표 목록을 조회할 수 있다.")
    void getMyVotedVotes_Authenticated_ReturnsVotes() throws Exception {
        mockMvc.perform(get("/votes/mine/voted")
                        .with(user(member1Id.toString()))
                        .param("category", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("비로그인 사용자도 공개 투표 상세를 조회할 수 있다.")
    void getVoteDetail_Anonymous_ReturnsVote() throws Exception {
        mockMvc.perform(get("/votes/{voteId}", hotIssueVoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteId").value(hotIssueVoteId));
    }

    @Test
    @DisplayName("반응성이 가장 높은 핫이슈 투표 정보를 성공적으로 조회한다.")
    void getHotIssueVote_Success() throws Exception {
        Vote hotIssueVote = voteRepository.findAll().stream()
                .filter(vote -> "오늘의 점심 선택은?".equals(vote.getTitle()))
                .findFirst()
                .orElseThrow();
        CommentGroup hotCommentGroup = commentGroupRepository.findByVoteId(hotIssueVote.getId())
                .orElseThrow();
        Comment parentComment = commentRepository.save(Comment.builder()
                .member(hotIssueVote.getMember())
                .commentGroup(hotCommentGroup)
                .content("최상위 댓글")
                .build());
        commentRepository.save(Comment.builder()
                .member(hotIssueVote.getMember())
                .commentGroup(hotCommentGroup)
                .parent(parentComment)
                .content("대댓글")
                .build());

        mockMvc.perform(get("/votes/trending")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreType").value("PERIOD"))
                .andExpect(jsonPath("$.fallbackApplied").value(false))
                .andExpect(jsonPath("$.votes[0].voteId").isNumber())
                .andExpect(jsonPath("$.votes[0].title").value("오늘의 점심 선택은?"))
                .andExpect(jsonPath("$.votes[0].category").value(VoteCategory.FOOD.name()))
                .andExpect(jsonPath("$.votes[0].reactivityScore").value(3))
                .andExpect(jsonPath("$.votes[0].voteReactionCount").value(1))
                .andExpect(jsonPath("$.votes[0].commentReactionCount").value(2))
                .andExpect(jsonPath("$.votes[0].totalParticipants").value(100))
                .andExpect(jsonPath("$.votes[0].createdBy").value("테스터1닉네임"))
                .andExpect(jsonPath("$.votes[0].options[0].content").value("A. 맵고 얼큰한 라면"))
                .andExpect(jsonPath("$.votes[0].options[0].vote_count").value(60));
    }

    @Test
    @DisplayName("트렌딩 투표가 없을 때 빈 목록을 반환한다.")
    void getHotIssueVote_NotFound() throws Exception {
        memberVoteOptionRepository.deleteAll();
        commentGroupRepository.deleteAll();
        voteRepository.deleteAll();

        mockMvc.perform(get("/votes/trending")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallbackApplied").value(true))
                .andExpect(jsonPath("$.votes").isEmpty());
    }

    @Test
    @DisplayName("트렌딩 조회 기간이 허용 범위를 벗어나면 400 Bad Request를 반환한다.")
    void getTrendingVotes_InvalidDays_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes/trending")
                        .param("days", "31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("조회 기간은 1일 이상 30일 이하여야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("트렌딩 조회 기간을 누락하면 400 Bad Request를 반환한다.")
    void getTrendingVotes_MissingDays_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes/trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("days 파라미터를 입력해주세요."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("고정 투표를 첫 번째에 배치하고 반응성 순위와 중복 없이 반환한다.")
    void getTrendingVotes_PinnedVoteComesFirst() throws Exception {
        Vote pinnedVote = voteRepository.findAll().stream()
                .filter(vote -> "연애 밸런스 게임".equals(vote.getTitle()))
                .findFirst()
                .orElseThrow();
        pinnedVote.pin(PinType.TRENDING);
        voteRepository.save(pinnedVote);

        mockMvc.perform(get("/votes/trending")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(2))
                .andExpect(jsonPath("$.votes[0].title").value("연애 밸런스 게임"))
                .andExpect(jsonPath("$.votes[0].displayType").value("PINNED"))
                .andExpect(jsonPath("$.votes[0].reactivityScore").value(0))
                .andExpect(jsonPath("$.votes[1].title").value("오늘의 점심 선택은?"))
                .andExpect(jsonPath("$.votes[1].displayType").value("RANKED"));
    }

    @Test
    @DisplayName("기존 best 엔드포인트는 제거된다.")
    void getBestVote_Removed() throws Exception {
        mockMvc.perform(get("/votes/best")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("동일한 반응성을 가진 투표 중 최신 투표를 조회한다.")
    void getHotIssueVote_SameTotalVoteCount_NewerIsHotIssue() throws Exception {
        memberVoteOptionRepository.deleteAll();
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
        CommentGroup oldCommentGroup = commentGroupRepository.save(
                CommentGroup.builder().vote(oldVote).totalCommentCount(1).build());

        Vote newVote = Vote.builder()
                .category(VoteCategory.LOVE).title("새로운 핫이슈 투표")
                .totalVoteCount(50).reactivityScore(50)
                .reactivityUpdatedAt(LocalDateTime.now())
                .member(member3).pinType(PinType.NONE).build();
        voteRepository.save(newVote);
        CommentGroup newCommentGroup = commentGroupRepository.save(
                CommentGroup.builder().vote(newVote).totalCommentCount(1).build());

        commentRepository.save(Comment.builder()
                .member(member3)
                .commentGroup(oldCommentGroup)
                .content("오래된 투표의 댓글")
                .build());
        commentRepository.save(Comment.builder()
                .member(member3)
                .commentGroup(newCommentGroup)
                .content("새로운 투표의 댓글")
                .build());

        mockMvc.perform(get("/votes/trending")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes[0].title").value("새로운 핫이슈 투표"))
                .andExpect(jsonPath("$.votes[0].totalParticipants").value(50))
                .andExpect(jsonPath("$.votes[0].createdBy").value("테스터3닉네임"));
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

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("투표 목록 조회 시 size가 1보다 작으면 400 Bad Request를 반환한다.")
    void getVotes_InvalidSize_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("size는 1 이상 50 이하여야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("투표 목록 조회 시 size가 50보다 크면 400 Bad Request를 반환한다.")
    void getVotes_SizeOverMaximum_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("size", "51")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("size는 1 이상 50 이하여야 합니다."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("투표 목록 조회 시 잘못된 category는 400 Bad Request를 반환한다.")
    void getVotes_InvalidCategory_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/votes")
                        .param("category", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("category는 ALL, FOOD, LOVE, BUY, SPORT, WORRY, ETC 중 하나여야 합니다."))
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

    @Test
    @WithMockUser(username = "0", roles = "ADMIN")
    @DisplayName("관리자 토큰 subject가 0이어도 투표 핀 설정에 성공한다.")
    void updatePinStatus_AdminSubjectZero_Success() throws Exception {
        Vote vote = voteRepository.findAll().get(0);

        mockMvc.perform(patch("/votes/{voteId}/pin", vote.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pinType": "TRENDING"
                                }
                                """))
                .andExpect(status().isOk());

        Vote pinnedVote = voteRepository.findById(vote.getId()).orElseThrow();
        assertThat(pinnedVote.getPinType()).isEqualTo(PinType.TRENDING);
    }
}
