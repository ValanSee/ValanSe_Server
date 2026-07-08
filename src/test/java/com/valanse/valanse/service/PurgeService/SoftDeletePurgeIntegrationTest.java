package com.valanse.valanse.service.PurgeService;

import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentLikeRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.service.StorageService.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SoftDeletePurgeIntegrationTest {
    @Autowired private SoftDeletePurgeService purgeService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private CommentLikeRepository commentLikeRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
    @MockBean private StorageService storageService;

    @Test
    void expiredCommentIsAnonymizedWithoutBreakingReplyTree() {
        Fixture fixture = createFixture();
        Comment parent = saveComment(fixture, null, "삭제할 부모 댓글");
        Comment reply = saveComment(fixture, parent, "유지할 대댓글");
        commentLikeRepository.saveAndFlush(CommentLike.builder()
                .user(fixture.member()).comment(parent).build());
        expire("comment", parent.getId());

        PurgeResult result = purgeService.purgeExpired(LocalDateTime.now());

        assertThat(result.commentsAnonymized()).isEqualTo(1);
        assertThat(count("comment", parent.getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select content from comment where id = ?", String.class, parent.getId()))
                .isEqualTo("삭제된 댓글입니다");
        assertThat(jdbcTemplate.queryForObject("select member_id from comment where id = ?", Long.class, parent.getId()))
                .isNull();
        assertThat(jdbcTemplate.queryForObject("select purged_at from comment where id = ?", Timestamp.class, parent.getId()))
                .isNotNull();
        assertThat(jdbcTemplate.queryForObject("select parent_id from comment where id = ?", Long.class, reply.getId()))
                .isEqualTo(parent.getId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from comment_like where comment_id = ?", Integer.class, parent.getId()))
                .isZero();
    }

    @Test
    void previewCountsExpiredRowsWithoutChangingData() {
        Fixture fixture = createFixture();
        Comment comment = saveComment(fixture, null, "dry-run으로 유지할 댓글");
        expire("comment", comment.getId());
        expire("vote", fixture.vote().getId());
        expire("member", fixture.member().getId());

        PurgePreview preview = purgeService.preview(LocalDateTime.now());

        assertThat(preview.expiredComments()).isEqualTo(1);
        assertThat(preview.expiredVotes()).isEqualTo(1);
        assertThat(preview.expiredMembers()).isEqualTo(1);
        assertThat(count("comment", comment.getId())).isEqualTo(1);
        assertThat(count("vote", fixture.vote().getId())).isEqualTo(1);
        assertThat(count("member", fixture.member().getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select content from comment where id = ?", String.class, comment.getId()))
                .isEqualTo("dry-run으로 유지할 댓글");
        assertThat(jdbcTemplate.queryForObject("select purged_at from comment where id = ?", Timestamp.class, comment.getId()))
                .isNull();
    }

    @Test
    void expiredVoteAndAllChildrenArePhysicallyDeleted() {
        Fixture fixture = createFixture();
        Comment parent = saveComment(fixture, null, "부모");
        saveComment(fixture, parent, "대댓글");
        commentLikeRepository.saveAndFlush(CommentLike.builder().user(fixture.member()).comment(parent).build());
        memberVoteOptionRepository.saveAndFlush(MemberVoteOption.builder()
                .member(fixture.member()).vote(fixture.vote()).voteOption(fixture.option()).build());
        expire("vote", fixture.vote().getId());

        PurgeResult result = purgeService.purgeExpired(LocalDateTime.now());

        assertThat(result.votesDeleted()).isEqualTo(1);
        assertThat(count("vote", fixture.vote().getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from vote_option where vote_id = ?", Integer.class, fixture.vote().getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from comment_group where vote_id = ?", Integer.class, fixture.vote().getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from member_vote_option where vote_id = ?", Integer.class, fixture.vote().getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from comment", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from comment_like", Integer.class)).isZero();
    }

    @Test
    void expiredMemberIsDeletedButAuthoredContentRemainsAnonymous() {
        Fixture fixture = createFixture();
        Comment comment = saveComment(fixture, null, "유지할 댓글");
        expire("member", fixture.member().getId());

        PurgeResult result = purgeService.purgeExpired(LocalDateTime.now());

        assertThat(result.membersDeleted()).isEqualTo(1);
        assertThat(count("member", fixture.member().getId())).isZero();
        assertThat(count("vote", fixture.vote().getId())).isEqualTo(1);
        assertThat(count("comment", comment.getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select member_id from vote where id = ?", Long.class, fixture.vote().getId())).isNull();
        assertThat(jdbcTemplate.queryForObject("select member_id from comment where id = ?", Long.class, comment.getId())).isNull();
        assertThat(jdbcTemplate.queryForObject("select content from comment where id = ?", String.class, comment.getId()))
                .isEqualTo("유지할 댓글");
    }

    private Fixture createFixture() {
        Member member = memberRepository.saveAndFlush(Member.builder()
                .socialId("social-" + System.nanoTime()).email("test@example.com").name("테스터").build());
        Vote vote = voteRepository.saveAndFlush(Vote.builder()
                .member(member).category(VoteCategory.ETC).title("투표").content("본문").build());
        VoteOption option = voteOptionRepository.saveAndFlush(VoteOption.builder()
                .vote(vote).content("선택지").label(VoteLabel.A).build());
        CommentGroup group = commentGroupRepository.saveAndFlush(CommentGroup.builder().vote(vote).build());
        return new Fixture(member, vote, option, group);
    }

    private Comment saveComment(Fixture fixture, Comment parent, String content) {
        return commentRepository.saveAndFlush(Comment.builder()
                .member(fixture.member()).commentGroup(fixture.group()).voteOption(fixture.option())
                .parent(parent).content(content).likeCount(0).replyCount(0).build());
    }

    private void expire(String table, Long id) {
        jdbcTemplate.update("update " + table + " set deleted_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(15)), id);
    }

    private int count(String table, Long id) {
        return jdbcTemplate.queryForObject("select count(*) from " + table + " where id = ?", Integer.class, id);
    }

    private record Fixture(Member member, Vote vote, VoteOption option, CommentGroup group) {}
}
