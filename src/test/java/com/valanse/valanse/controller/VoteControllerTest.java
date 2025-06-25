// src/test/java/com/valanse/valanse/controller/VoteControllerTest.java
package com.valanse.valanse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valanse.valanse.domain.Member; //
import com.valanse.valanse.domain.MemberProfile; //
import com.valanse.valanse.domain.Vote; //
import com.valanse.valanse.domain.VoteOption; //
import com.valanse.valanse.domain.enums.Age; //
import com.valanse.valanse.domain.enums.Gender; //
import com.valanse.valanse.domain.enums.VoteCategory; //
import com.valanse.valanse.domain.enums.VoteLabel; //
import com.valanse.valanse.repository.MemberProfileRepository; //
import com.valanse.valanse.repository.MemberRepository; //
import com.valanse.valanse.repository.VoteRepository;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // Spring Boot 테스트 환경 로드
@AutoConfigureMockMvc // MockMvc 자동 구성
@ActiveProfiles("test") // application-test.yml 프로파일 활성화
@Transactional // 각 테스트 메서드가 끝날 때 트랜잭션 롤백
public class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 데 사용

    @Autowired
    private ObjectMapper objectMapper; // JSON 직렬화/역직렬화를 위한 유틸리티

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private MemberRepository memberRepository; // Member 저장을 위해 필요

    @Autowired
    private MemberProfileRepository memberProfileRepository; // MemberProfile 저장을 위해 필요

    @BeforeEach // 각 테스트 메서드 실행 전에 실행
    void setUp() {
        // 데이터 클린업 (Transactional 어노테이션으로 롤백되므로 필수는 아니지만 명시적으로 초기화)
        voteRepository.deleteAll();
        memberProfileRepository.deleteAll();
        memberRepository.deleteAll();

        // 핫이슈 투표 생성 시 필요한 멤버 및 프로필 데이터 생성
        Member member1 = Member.builder() //
                .socialId("kakao123")
                .email("test1@example.com")
                .name("테스터1")
                .profile_image_url("http://image.com/test1.jpg")
                .kakaoAccessToken("token1")
                .kakaoRefreshToken("refresh1")
                .build();
        memberRepository.save(member1); //

        MemberProfile profile1 = MemberProfile.builder() //
                .member(member1)
                .nickname("테스터1닉네임")
                .gender(Gender.M) //
                .age(Age.TWENTY) //
                .mbti("ENFP")
                .build();
        memberProfileRepository.save(profile1); //

        // 투표 생성 - 핫이슈 (가장 높은 totalVoteCount)
        Vote hotIssueVote = Vote.builder() //
                .category(VoteCategory.FOOD) // <-- 88번 줄: VoteCategory enum 값을 올바르게 할당
                .title("오늘의 점심 선택은?")
                .totalVoteCount(100)
                .member(member1) //
                .build();
        voteRepository.save(hotIssueVote);


        // 핫이슈 투표 옵션들
        VoteOption optionA = VoteOption.builder() //
                .vote(hotIssueVote)
                .content("A. 맵고 얼큰한 라면")
                .voteCount(60)
                .label(VoteLabel.A) //
                .build();
        VoteOption optionB = VoteOption.builder() //
                .vote(hotIssueVote)
                .content("B. 부드러운 파스타")
                .voteCount(40)
                .label(VoteLabel.B) //
                .build();
        hotIssueVote.getVoteOptions().addAll(Arrays.asList(optionA, optionB));
        voteRepository.save(hotIssueVote); // 옵션 추가 후 다시 저장

        // 다른 투표 생성 (totalVoteCount가 더 낮은 투표)
        Member member2 = Member.builder() //
                .socialId("kakao456")
                .email("test2@example.com")
                .name("테스터2")
                .profile_image_url("http://image.com/test2.jpg")
                .kakaoAccessToken("token2")
                .kakaoRefreshToken("refresh2")
                .build();
        memberRepository.save(member2); //

        MemberProfile profile2 = MemberProfile.builder() //
                .member(member2)
                .nickname("테스터2닉네임")
                .gender(Gender.F) //
                .age(Age.THIRTY) //
                .mbti("ISTJ")
                .build();
        memberProfileRepository.save(profile2); //

        // 다른 투표 생성 (totalVoteCount가 더 낮은 투표)
        Vote otherVote = Vote.builder() //
                .category(VoteCategory.LOVE) // <-- 133번 줄: VoteCategory enum 값을 올바르게 할당
                .title("연애 밸런스 게임")
                .totalVoteCount(50)
                .member(member2) //
                .build();
        voteRepository.save(otherVote);
    }

    @Test
    @DisplayName("가장 많이 참여한 핫이슈 투표 정보를 성공적으로 조회한다.")
    void getHotIssueVote_Success() throws Exception {
        mockMvc.perform(get("/votes/best") // GET 요청
                        .contentType(MediaType.APPLICATION_JSON)) // 요청 타입
                .andExpect(status().isOk()) // HTTP 상태 코드 200 OK 확인
                .andExpect(jsonPath("$.voteId").isNumber()) // voteId가 숫자인지 확인
                .andExpect(jsonPath("$.title").value("오늘의 점심 선택은?")) // 제목 확인
                .andExpect(jsonPath("$.category").value(VoteCategory.FOOD.name())) // 카테고리 확인
                .andExpect(jsonPath("$.totalParticipants").value(100)) // 총 투표수 확인
                .andExpect(jsonPath("$.createdBy").value("테스터1닉네임")) // 생성자 닉네임 확인
                .andExpect(jsonPath("$.options[0].content").value("A. 맵고 얼큰한 라면")) // 첫 번째 옵션 내용 확인
                .andExpect(jsonPath("$.options[0].vote_count").value(60)) // 첫 번째 옵션 투표 수 확인
                .andExpect(jsonPath("$.options[1].content").value("B. 부드러운 파스타")) // 두 번째 옵션 내용 확인
                .andExpect(jsonPath("$.options[1].vote_count").value(40)); // 두 번째 옵션 투표 수 확인
    }

    @Test
    @DisplayName("핫이슈 투표가 없을 때 404 Not Found를 반환한다.")
    void getHotIssueVote_NotFound() throws Exception {
        // 모든 투표 삭제하여 핫이슈 투표가 없는 상태로 만듦
        voteRepository.deleteAll();

        mockMvc.perform(get("/votes/best") // GET 요청
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()) // HTTP 상태 코드 404 Not Found 확인
                .andExpect(jsonPath("$.error").value("핫이슈 투표를 찾을 수 없습니다.")) // 에러 메시지 확인
                .andExpect(jsonPath("$.status").value(404)) // 상태 코드 확인
                .andExpect(jsonPath("$.type").value("ApiException")); // 예외 타입 확인
    }

    @Test
    @DisplayName("동일한 totalVoteCount를 가진 투표 중 최신 투표를 조회한다.")
    void getHotIssueVote_SameTotalVoteCount_NewerIsHotIssue() throws Exception {
        // 데이터 클린업 (setUp에서 생성된 데이터 제거)
        voteRepository.deleteAll();

        // 맴버 생성
        Member member3 = Member.builder() //
                .socialId("kakao789")
                .email("test3@example.com")
                .name("테스터3")
                .profile_image_url("http://image.com/test3.jpg")
                .kakaoAccessToken("token3")
                .kakaoRefreshToken("refresh3")
                .build();
        memberRepository.save(member3); //

        MemberProfile profile3 = MemberProfile.builder() //
                .member(member3)
                .nickname("테스터3닉네임")
                .gender(Gender.M) //
                .age(Age.FORTY) //
                .mbti("INTP")
                .build();
        memberProfileRepository.save(profile3); //


        // 오래된 투표 (totalVoteCount: 50, createdAt: 3일 전)
        Vote oldVote = Vote.builder() //
                .category(VoteCategory.ETC) //
                .title("오래된 핫이슈 투표")
                .totalVoteCount(50)
                .member(member3) //
                .build();
        voteRepository.save(oldVote);

        // 새로운 투표 (totalVoteCount: 50, createdAt: 1일 전) - 이것이 핫이슈로 선택되어야 함
        Vote newVote = Vote.builder() //
                .category(VoteCategory.LOVE) //
                .title("새로운 핫이슈 투표")
                .totalVoteCount(50)
                .member(member3) //
                .build();
        voteRepository.save(newVote);

        mockMvc.perform(get("/votes/best") // GET 요청
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("새로운 핫이슈 투표")) // 최신 투표가 선택되었는지 확인
                .andExpect(jsonPath("$.totalParticipants").value(50))
                .andExpect(jsonPath("$.createdBy").value("테스터3닉네임"));
    }
}