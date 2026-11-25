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
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.TitleRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 탈퇴(soft delete)한 회원의 투표·댓글·대댓글이 퍼지 전 14일 동안에도 퍼지 이후와 똑같이
 * "탈퇴한 사용자"로 익명화돼(authorId/칭호 null, isBot false) 노출되는지 검증하는 통합 테스트입니다.
 * 활동 중인 작성자는 그대로, 이미 퍼지된(member null) 콘텐츠도 기존처럼 동작해야 한다.
 * (JSONPath 필터식은 배열을 반환하므로 null 검증은 contains(nullValue())로 한다.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WithdrawnAuthorAnonymizationIntegrationTest {

    private static final String WITHDRAWN = "탈퇴한 사용자";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private TitleRepository titleRepository;
    @Autowired private MemberProfileTitleRepository memberProfileTitleRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;

    private Member active;
    private Member withdrawn;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        memberVoteOptionRepository.deleteAll();
        commentGroupRepository.deleteAll();
        voteOptionRepository.deleteAll();
        voteRepository.deleteAll();
        memberProfileTitleRepository.deleteAll();
        titleRepository.deleteAll();
        memberProfileRepository.deleteAll();
        memberRepository.deleteAll();

        active = saveMemberWithTitle("active-author", "활동회원", "active-title", "활동 칭호");
        withdrawn = saveMemberWithTitle("withdrawn-author", "탈퇴예정회원", "withdrawn-title", "탈퇴자 칭호");

        // MemberServiceImpl.deleteMemberById()와 동일하게 Member/MemberProfile을 soft delete 한다.
        withdrawn.softDelete();
        memberProfileRepository.findByMemberId(withdrawn.getId()).orElseThrow().softDelete();
    }

    @Test
    @DisplayName("GET /votes - 탈퇴 작성자는 익명화, 활동 작성자는 그대로, 퍼지된 투표도 정상")
    void voteList() throws Exception {
        Vote activeVote = saveVote(active, "활동 회원 투표");
        Vote withdrawnVote = saveVote(withdrawn, "탈퇴 회원 투표");
        Vote purgedVote = saveVote(null, "퍼지된 투표");
        flushAndClear();

        mockMvc.perform(get("/votes").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(3))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].nickname", withdrawnVote.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].authorId", withdrawnVote.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].member_id", withdrawnVote.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].member_title", withdrawnVote.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].isBot", withdrawnVote.getId()).value(false))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].title", withdrawnVote.getId()).value("탈퇴 회원 투표"))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].nickname", activeVote.getId()).value("활동회원"))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].authorId", activeVote.getId()).value(active.getId().intValue()))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].member_title", activeVote.getId()).value("활동 칭호"))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].nickname", purgedVote.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.votes[?(@.id == %d)].authorId", purgedVote.getId()).value(contains(nullValue())));
    }

    @Test
    @DisplayName("GET /votes/trending - 탈퇴 작성자는 익명화, 활동 작성자는 그대로, 퍼지된 투표도 정상")
    void trending() throws Exception {
        Vote activeVote = saveVote(active, "활동 회원 투표");
        Vote withdrawnVote = saveVote(withdrawn, "탈퇴 회원 투표");
        Vote purgedVote = saveVote(null, "퍼지된 투표");
        flushAndClear();

        // 기간 내 활동이 없으므로 전체 기간(fallback) 랭킹에 세 투표가 모두 포함된다.
        mockMvc.perform(get("/votes/trending").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes.length()").value(3))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].createdBy", withdrawnVote.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].authorId", withdrawnVote.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].creatorTitle", withdrawnVote.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].isBot", withdrawnVote.getId()).value(false))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].createdBy", activeVote.getId()).value("활동회원"))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].authorId", activeVote.getId()).value(active.getId().intValue()))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].creatorTitle", activeVote.getId()).value("활동 칭호"))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].createdBy", purgedVote.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.votes[?(@.voteId == %d)].authorId", purgedVote.getId()).value(contains(nullValue())));
    }

    @Test
    @DisplayName("GET /votes/{voteId} - 탈퇴 작성자는 익명화, 활동 작성자는 그대로, 퍼지된 투표도 정상")
    void voteDetail() throws Exception {
        Vote activeVote = saveVote(active, "활동 회원 투표");
        Vote withdrawnVote = saveVote(withdrawn, "탈퇴 회원 투표");
        Vote purgedVote = saveVote(null, "퍼지된 투표");
        flushAndClear();

        mockMvc.perform(get("/votes/{voteId}", withdrawnVote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("탈퇴 회원 투표"))
                .andExpect(jsonPath("$.creatorNickname").value(WITHDRAWN))
                .andExpect(jsonPath("$.authorId").value(nullValue()))
                .andExpect(jsonPath("$.creatorTitle").value(nullValue()))
                .andExpect(jsonPath("$.isBot").value(false));

        mockMvc.perform(get("/votes/{voteId}", activeVote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatorNickname").value("활동회원"))
                .andExpect(jsonPath("$.authorId").value(active.getId().intValue()))
                .andExpect(jsonPath("$.creatorTitle").value("활동 칭호"));

        mockMvc.perform(get("/votes/{voteId}", purgedVote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatorNickname").value(WITHDRAWN))
                .andExpect(jsonPath("$.authorId").value(nullValue()))
                .andExpect(jsonPath("$.creatorTitle").value(nullValue()));
    }

    @Test
    @DisplayName("GET /votes/{voteId}/comments - 탈퇴 작성자는 익명화(선택지 라벨은 유지), 활동/퍼지 댓글은 그대로")
    void comments() throws Exception {
        Vote vote = saveVote(active, "투표");
        CommentGroup group = commentGroupRepository.save(CommentGroup.builder().vote(vote).totalCommentCount(0).build());
        VoteOption optionA = vote.getVoteOptions().get(0);
        memberVoteOptionRepository.save(MemberVoteOption.builder()
                .member(withdrawn).vote(vote).voteOption(optionA).build());

        Comment activeComment = saveComment(active, group, null, "활동 회원 댓글");
        Comment withdrawnComment = saveComment(withdrawn, group, null, "탈퇴 회원 댓글");
        Comment purgedComment = saveComment(null, group, null, "퍼지된 댓글");
        flushAndClear();

        mockMvc.perform(get("/votes/{voteId}/comments", vote.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(3))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].content", withdrawnComment.getId()).value("탈퇴 회원 댓글"))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].nickname", withdrawnComment.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].authorId", withdrawnComment.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].title", withdrawnComment.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].isBot", withdrawnComment.getId()).value(false))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].voteOptionLabel", withdrawnComment.getId()).value("A"))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].nickname", activeComment.getId()).value("활동회원"))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].authorId", activeComment.getId()).value(active.getId().intValue()))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].title", activeComment.getId()).value("활동 칭호"))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].nickname", purgedComment.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.comments[?(@.commentId == %d)].authorId", purgedComment.getId()).value(contains(nullValue())));
    }

    @Test
    @DisplayName("GET /votes/{voteId}/comments/{id}/replies - 탈퇴 작성자는 익명화, 활동/퍼지 대댓글은 그대로")
    void replies() throws Exception {
        Vote vote = saveVote(active, "투표");
        CommentGroup group = commentGroupRepository.save(CommentGroup.builder().vote(vote).totalCommentCount(0).build());
        Comment parent = saveComment(active, group, null, "부모 댓글");
        Comment activeReply = saveComment(active, group, parent, "활동 회원 대댓글");
        Comment withdrawnReply = saveComment(withdrawn, group, parent, "탈퇴 회원 대댓글");
        Comment purgedReply = saveComment(null, group, parent, "퍼지된 대댓글");
        flushAndClear();

        mockMvc.perform(get("/votes/{voteId}/comments/{commentId}/replies", vote.getId(), parent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies.length()").value(3))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].content", withdrawnReply.getId()).value("탈퇴 회원 대댓글"))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].nickname", withdrawnReply.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].authorId", withdrawnReply.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].title", withdrawnReply.getId()).value(contains(nullValue())))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].isBot", withdrawnReply.getId()).value(false))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].nickname", activeReply.getId()).value("활동회원"))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].authorId", activeReply.getId()).value(active.getId().intValue()))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].title", activeReply.getId()).value("활동 칭호"))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].nickname", purgedReply.getId()).value(WITHDRAWN))
                .andExpect(jsonPath("$.replies[?(@.id == %d)].authorId", purgedReply.getId()).value(contains(nullValue())));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Member saveMemberWithTitle(String socialId, String nickname, String titleCode, String titleName) {
        Member member = memberRepository.save(Member.builder()
                .socialId(socialId).email(socialId + "@example.com").name(socialId)
                .nickname(nickname).isBot(false).build());
        MemberProfile profile = memberProfileRepository.save(MemberProfile.builder()
                .member(member).nickname(nickname).gender(Gender.MALE).age(Age.TWENTY).mbti("ENFP").build());
        Title title = titleRepository.save(Title.builder()
                .code(titleCode).name(titleName).description("테스트 칭호")
                .price(0L).tier(TitleTier.BASIC).acquisitionType(TitleAcquisitionType.DEFAULT)
                .active(true).displayOrder(0)
                .build());
        memberProfileTitleRepository.save(MemberProfileTitle.builder()
                .memberProfile(profile).title(title).equipped(true).build());
        return member;
    }

    private Vote saveVote(Member author, String title) {
        Vote vote = voteRepository.save(Vote.builder()
                .member(author).title(title).content("content")
                .category(VoteCategory.FOOD).totalVoteCount(0).pinType(PinType.NONE)
                .build());
        VoteOption optionA = voteOptionRepository.save(VoteOption.builder()
                .vote(vote).content("A").label(VoteLabel.A).voteCount(0).build());
        VoteOption optionB = voteOptionRepository.save(VoteOption.builder()
                .vote(vote).content("B").label(VoteLabel.B).voteCount(0).build());
        vote.getVoteOptions().add(optionA);
        vote.getVoteOptions().add(optionB);
        return vote;
    }

    private Comment saveComment(Member author, CommentGroup group, Comment parent, String content) {
        return commentRepository.save(Comment.builder()
                .content(content).member(author).commentGroup(group).parent(parent)
                .likeCount(0).replyCount(0).build());
    }
}
