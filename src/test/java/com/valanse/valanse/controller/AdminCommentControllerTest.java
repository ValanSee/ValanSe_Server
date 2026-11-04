package com.valanse.valanse.controller;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;

    private Vote vote;
    private CommentGroup commentGroup;
    private Member humanAuthor;
    private Member botAuthor;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        commentGroupRepository.deleteAll();
        voteOptionRepository.deleteAll();
        voteRepository.deleteAll();
        memberProfileRepository.deleteAll();
        memberRepository.deleteAll();

        humanAuthor = saveMember("human-author", false, "사람작성자");
        botAuthor = saveMember("bot-author", true, "봇작성자");

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
    @DisplayName("비로그인 사용자는 관리자 댓글 검수 API를 호출할 수 없다")
    void getAdminComments_AnonymousUser_IsForbidden() throws Exception {
        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자는 관리자 댓글 검수 API를 호출할 수 없다")
    void getAdminComments_UserRole_IsForbidden() throws Exception {
        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("부모 댓글과 대댓글을 하나의 목록으로 최신순 조회한다")
    void getAdminComments_ReturnsParentAndReplyTogetherInLatestOrder() throws Exception {
        Comment parent = saveComment(humanAuthor, null, "부모 댓글");
        Comment reply = saveComment(botAuthor, parent, "대댓글");

        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].commentId").value(reply.getId()))
                .andExpect(jsonPath("$.comments[0].isReply").value(true))
                .andExpect(jsonPath("$.comments[0].parentCommentId").value(parent.getId()))
                .andExpect(jsonPath("$.comments[0].voteId").value(vote.getId()))
                .andExpect(jsonPath("$.comments[0].voteTitle").value(vote.getTitle()))
                .andExpect(jsonPath("$.comments[0].isBot").value(true))
                .andExpect(jsonPath("$.comments[1].commentId").value(parent.getId()))
                .andExpect(jsonPath("$.comments[1].isReply").value(false))
                .andExpect(jsonPath("$.comments[1].parentCommentId").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(30))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("삭제된 댓글은 목록에서 제외된다")
    void getAdminComments_ExcludesDeletedComment() throws Exception {
        Comment visible = saveComment(humanAuthor, null, "살아있는 댓글");
        Comment deleted = saveComment(humanAuthor, null, "삭제될 댓글");
        deleted.markDeleted();
        commentRepository.save(deleted);

        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(visible.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("삭제된 투표에 속한 댓글은 목록에서 제외된다")
    void getAdminComments_ExcludesCommentsOfDeletedVote() throws Exception {
        saveComment(humanAuthor, null, "살아있는 투표의 댓글");

        Vote deletedVote = voteRepository.save(Vote.builder()
                .member(humanAuthor).title("삭제될 투표").content("content")
                .category(VoteCategory.FOOD).totalVoteCount(0).pinType(PinType.NONE)
                .build());
        CommentGroup deletedVoteGroup = commentGroupRepository.save(
                CommentGroup.builder().vote(deletedVote).totalCommentCount(0).build());
        commentRepository.save(Comment.builder()
                .content("삭제된 투표에 달린 댓글").member(humanAuthor).commentGroup(deletedVoteGroup)
                .likeCount(0).replyCount(0).build());
        deletedVote.softDelete();
        voteRepository.save(deletedVote);

        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].voteId").value(vote.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("isBot=true면 봇 작성 댓글만 조회한다")
    void getAdminComments_IsBotTrue_ReturnsOnlyBotComments() throws Exception {
        saveComment(humanAuthor, null, "사람 댓글");
        Comment botComment = saveComment(botAuthor, null, "봇 댓글");

        mockMvc.perform(get("/admin/comments").param("isBot", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(botComment.getId()))
                .andExpect(jsonPath("$.comments[0].isBot").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("isBot=false면 일반 회원 작성 댓글만 조회한다")
    void getAdminComments_IsBotFalse_ReturnsOnlyHumanComments() throws Exception {
        Comment humanComment = saveComment(humanAuthor, null, "사람 댓글");
        saveComment(botAuthor, null, "봇 댓글");

        mockMvc.perform(get("/admin/comments").param("isBot", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(humanComment.getId()))
                .andExpect(jsonPath("$.comments[0].isBot").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("isBot=false면 회원 정보가 null로 정리된(탈퇴/퍼지된) 댓글도 포함한다")
    void getAdminComments_IsBotFalse_IncludesWithdrawnMemberComments() throws Exception {
        // SoftDeletePurgeService.purgeMembers()가 14일 보존기간 후 member_id를 null로
        // 정리한다 - 이 상태를 commentRepository.save(... .member(null) ...)로 재현한다.
        Comment withdrawnComment = commentRepository.save(Comment.builder()
                .content("탈퇴 회원 댓글").member(null).commentGroup(commentGroup)
                .likeCount(0).replyCount(0).build());
        saveComment(botAuthor, null, "봇 댓글");

        mockMvc.perform(get("/admin/comments").param("isBot", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value(withdrawnComment.getId()))
                .andExpect(jsonPath("$.comments[0].isBot").value(false))
                .andExpect(jsonPath("$.comments[0].nickname").value("탈퇴한 사용자"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("page/size로 페이지네이션하고 다음 페이지 여부를 반환한다")
    void getAdminComments_SupportsPagination() throws Exception {
        for (int i = 0; i < 3; i++) {
            saveComment(humanAuthor, null, "댓글 " + i);
        }

        mockMvc.perform(get("/admin/comments").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/admin/comments").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    private Member saveMember(String socialId, boolean isBot, String nickname) {
        Member member = memberRepository.save(Member.builder()
                .socialId(socialId).email(socialId + "@example.com").name(socialId)
                .nickname(nickname).isBot(isBot).build());
        memberProfileRepository.save(MemberProfile.builder()
                .member(member).nickname(nickname).gender(Gender.MALE).age(Age.TWENTY).mbti("ENFP").build());
        return member;
    }

    private Comment saveComment(Member author, Comment parent, String content) {
        return commentRepository.save(Comment.builder()
                .content(content).member(author).commentGroup(commentGroup)
                .parent(parent).likeCount(0).replyCount(0).build());
    }
}
