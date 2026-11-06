package com.valanse.valanse.controller;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.TitleRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M4(REQ-010/011/AC-019) 통합 테스트입니다. 콘텐츠 시드 봇에게 직접 INSERT로
 * 부여된 member_profile_title(equipped=true)이, 관리자(CMS)가 실제로 조회하는
 * 댓글 응답 경로(GET /votes/{voteId}/comments, CommentRepositoryImpl의
 * title.name LEFT JOIN)에서 null이 아닌 실제 칭호명으로 표시되는지 검증한다.
 * AdminCommentControllerTest와 동일한 @SpringBootTest + MockMvc 패턴입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BotTitleDisplayIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private TitleRepository titleRepository;
    @Autowired private MemberProfileTitleRepository memberProfileTitleRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;

    private Vote vote;
    private CommentGroup commentGroup;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        commentGroupRepository.deleteAll();
        voteOptionRepository.deleteAll();
        voteRepository.deleteAll();
        memberProfileTitleRepository.deleteAll();
        titleRepository.deleteAll();
        memberProfileRepository.deleteAll();
        memberRepository.deleteAll();

        Member humanAuthor = saveMember("human-author", false, "사람작성자");
        vote = voteRepository.save(Vote.builder()
                .member(humanAuthor).title("오늘의 점심 선택은?").content("content")
                .category(VoteCategory.FOOD).totalVoteCount(0).pinType(PinType.NONE)
                .build());
        voteOptionRepository.save(VoteOption.builder()
                .vote(vote).content("A").label(VoteLabel.A).voteCount(0).build());
        voteOptionRepository.save(VoteOption.builder()
                .vote(vote).content("B").label(VoteLabel.B).voteCount(0).build());
        commentGroup = commentGroupRepository.save(CommentGroup.builder().vote(vote).totalCommentCount(0).build());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    @DisplayName("봇에게 직접 부여·장착된 칭호가 CMS 댓글 조회 응답에 null 없이 표시된다")
    void getComments_BotWithGrantedTitle_ShowsTitleNotNull() throws Exception {
        Member bot = saveMember("content-seed-bot-001", true, "한라봉");
        grantAndEquipTitle(bot, "bot-curator", "AI 큐레이터");

        Comment botComment = commentRepository.save(Comment.builder()
                .content("이 중에 고민이면 그냥 둘 다 드세요").member(bot).commentGroup(commentGroup)
                .likeCount(0).replyCount(0).build());

        mockMvc.perform(get("/votes/{voteId}/comments", vote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(botComment.getId()))
                .andExpect(jsonPath("$.comments[0].isBot").value(true))
                .andExpect(jsonPath("$.comments[0].title").value("AI 큐레이터"));
    }

    // 칭호가 없는(장착하지 않은) 작성자의 댓글은 title 필드가 null이어야 한다 -
    // 위 테스트가 "항상 null이 아닌 값이 채워지는" 버그가 아니라, 실제로
    // member_profile_title 데이터를 반영한 결과임을 구분해 보여준다.
    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    @DisplayName("칭호를 장착하지 않은 작성자의 댓글은 title 필드가 null이다")
    void getComments_AuthorWithoutTitle_ShowsTitleNull() throws Exception {
        Member bot = saveMember("content-seed-bot-002", true, "존이냐");

        Comment botComment = commentRepository.save(Comment.builder()
                .content("저도 그거 궁금했어요").member(bot).commentGroup(commentGroup)
                .likeCount(0).replyCount(0).build());

        mockMvc.perform(get("/votes/{voteId}/comments", vote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(botComment.getId()))
                .andExpect(jsonPath("$.comments[0].title").value(nullValue()));
    }

    private Member saveMember(String socialId, boolean isBot, String nickname) {
        Member member = memberRepository.save(Member.builder()
                .socialId(socialId).email(socialId + "@example.com").name(socialId)
                .nickname(nickname).isBot(isBot).build());
        memberProfileRepository.save(MemberProfile.builder()
                .member(member).nickname(nickname).gender(Gender.MALE).age(Age.TWENTY).mbti("ENFP").build());
        return member;
    }

    private void grantAndEquipTitle(Member member, String code, String name) {
        Title title = titleRepository.save(Title.builder()
                .code(code).name(name).description("테스트용 봇 칭호")
                .price(0L).tier(TitleTier.BASIC).acquisitionType(TitleAcquisitionType.DEFAULT)
                .active(false).displayOrder(0)
                .build());
        MemberProfile profile = memberProfileRepository.findByMemberId(member.getId()).orElseThrow();
        memberProfileTitleRepository.save(MemberProfileTitle.builder()
                .memberProfile(profile).title(title).equipped(true).build());
    }
}
