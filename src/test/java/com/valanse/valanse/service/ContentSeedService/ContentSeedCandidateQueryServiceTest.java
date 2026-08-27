package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR3-1 "생성 대상 조회"의 규칙(최근 활성 게시글, 7일 이내 활성 게시글, 우선순위,
 * 자기 글/기존 투표/기존 댓글 제외, 최근 최상위 댓글 첨부, 게시글당 자동 댓글 제한)이
 * 실제로 지켜지는지 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContentSeedCandidateQueryServiceTest {

    @Autowired private ContentSeedCandidateQueryService candidateQueryService;
    @Autowired private ContentSeedProperties properties;

    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUpBefore() {
        cleanUp();
    }

    @AfterEach
    void cleanUpAfter() {
        cleanUp();
    }

    @Test
    @DisplayName("최근 활성 게시글을 최신순으로 recentTitleLimit개까지만 반환한다")
    void findRecentActivePosts_LimitsToConfiguredCount() {
        int limit = properties.getRecentTitleLimit();
        Member creator = saveMember("recent-creator", false);
        for (int i = 0; i < limit + 3; i++) {
            saveVoteWithOptions(creator, "title-" + i, VoteCategory.FOOD, 2);
        }

        List<RecentPost> result = candidateQueryService.findRecentActivePosts();

        assertThat(result).hasSize(limit);
    }

    @Test
    @DisplayName("ALL 카테고리 게시글은 참고 목록에서 제외한다")
    void findRecentActivePosts_ExcludesAllCategory() {
        Member creator = saveMember("all-category-creator", false);
        saveVoteWithOptions(creator, "all-category-title", VoteCategory.ALL, 2);
        saveVoteWithOptions(creator, "food-title", VoteCategory.FOOD, 2);

        List<RecentPost> result = candidateQueryService.findRecentActivePosts();

        assertThat(result).extracting(RecentPost::title).containsExactly("food-title");
    }

    @Test
    @DisplayName("targetVoteLookbackDays보다 오래된 게시글은 상호작용 후보에서 제외한다")
    void findInteractionCandidates_ExcludesOldVotes() {
        Member creator = saveMember("lookback-creator", false);
        Vote recentVote = saveVoteWithOptions(creator, "recent", VoteCategory.FOOD, 2);
        setVoteCreatedAt(recentVote.getId(), LocalDateTime.now().minusDays(3));

        Vote oldVote = saveVoteWithOptions(creator, "old", VoteCategory.FOOD, 2);
        setVoteCreatedAt(oldVote.getId(), LocalDateTime.now().minusDays(properties.getTargetVoteLookbackDays() + 1));

        Member bot = saveMember("content-seed-bot-101", true);

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(bot.getId(), Set.of(bot.getId()));

        assertThat(result).extracting(CandidatePost::id).containsExactly(recentVote.getId());
    }

    @Test
    @DisplayName("봇 자신이 작성한 게시글은 후보에서 제외한다")
    void findInteractionCandidates_ExcludesSelfAuthoredVote() {
        Member bot = saveMember("content-seed-bot-102", true);
        Vote ownVote = saveVoteWithOptions(bot, "own-vote", VoteCategory.FOOD, 2);
        setVoteCreatedAt(ownVote.getId(), LocalDateTime.now().minusDays(1));

        Member other = saveMember("other-creator", false);
        Vote othersVote = saveVoteWithOptions(other, "others-vote", VoteCategory.FOOD, 2);
        setVoteCreatedAt(othersVote.getId(), LocalDateTime.now().minusDays(1));

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(bot.getId(), Set.of(bot.getId()));

        assertThat(result).extracting(CandidatePost::id).containsExactly(othersVote.getId());
    }

    @Test
    @DisplayName("봇이 이미 투표한 게시글은 후보에서 제외한다")
    void findInteractionCandidates_ExcludesAlreadyVotedVote() {
        Member creator = saveMember("already-voted-creator", false);
        Vote vote = saveVoteWithOptions(creator, "already-voted", VoteCategory.FOOD, 2);
        setVoteCreatedAt(vote.getId(), LocalDateTime.now().minusDays(1));

        Member bot = saveMember("content-seed-bot-103", true);
        saveMemberVoteOption(bot, vote, vote.getVoteOptions().get(0));

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(bot.getId(), Set.of(bot.getId()));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("봇이 이미 댓글을 남긴 게시글은 후보에서 제외한다")
    void findInteractionCandidates_ExcludesAlreadyCommentedVote() {
        Member creator = saveMember("already-commented-creator", false);
        Vote vote = saveVoteWithOptions(creator, "already-commented", VoteCategory.FOOD, 2);
        setVoteCreatedAt(vote.getId(), LocalDateTime.now().minusDays(1));
        CommentGroup group = saveCommentGroup(vote);

        Member bot = saveMember("content-seed-bot-104", true);
        saveTopLevelComment(vote, group, bot, "이미 남긴 댓글");

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(bot.getId(), Set.of(bot.getId()));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("봇 댓글이 maxCommentsPerVote만큼 이미 달린 게시글은 후보에서 제외한다")
    void findInteractionCandidates_ExcludesVoteAtBotCommentLimit() {
        Member creator = saveMember("comment-limit-creator", false);
        Vote vote = saveVoteWithOptions(creator, "comment-limit", VoteCategory.FOOD, 2);
        setVoteCreatedAt(vote.getId(), LocalDateTime.now().minusDays(1));
        CommentGroup group = saveCommentGroup(vote);

        for (int i = 0; i < properties.getMaxCommentsPerVote(); i++) {
            Member otherBot = saveMember("content-seed-bot-limit-" + i, true);
            saveTopLevelComment(vote, group, otherBot, "봇 댓글 " + i);
        }

        Member bot = saveMember("content-seed-bot-105", true);

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(bot.getId(), Set.of(bot.getId()));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("선택지가 2개(A, B)가 아닌 게시글은 후보에서 제외한다")
    void findInteractionCandidates_ExcludesVotesWithoutExactlyTwoOptions() {
        Member creator = saveMember("option-count-creator", false);
        Vote threeOptionVote = saveVoteWithOptions(creator, "three-options", VoteCategory.FOOD, 3);
        setVoteCreatedAt(threeOptionVote.getId(), LocalDateTime.now().minusDays(1));

        Vote twoOptionVote = saveVoteWithOptions(creator, "two-options", VoteCategory.FOOD, 2);
        setVoteCreatedAt(twoOptionVote.getId(), LocalDateTime.now().minusDays(1));

        Member bot = saveMember("content-seed-bot-106", true);

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(bot.getId(), Set.of(bot.getId()));

        assertThat(result).extracting(CandidatePost::id).containsExactly(twoOptionVote.getId());
    }

    @Test
    @DisplayName("우선순위는 실제 사용자 글 -> 이번 실행 봇 글 -> 기존 봇 글 순이다")
    void findInteractionCandidates_OrdersByAuthorPriority() {
        Member human = saveMember("priority-human", false);
        Vote humanVote = saveVoteWithOptions(human, "human-vote", VoteCategory.FOOD, 2);
        setVoteCreatedAt(humanVote.getId(), LocalDateTime.now().minusDays(3));

        Member thisRunBot = saveMember("content-seed-bot-this-run", true);
        Vote thisRunBotVote = saveVoteWithOptions(thisRunBot, "this-run-bot-vote", VoteCategory.FOOD, 2);
        setVoteCreatedAt(thisRunBotVote.getId(), LocalDateTime.now().minusDays(2));

        Member otherBot = saveMember("content-seed-bot-other-run", true);
        Vote otherBotVote = saveVoteWithOptions(otherBot, "other-bot-vote", VoteCategory.FOOD, 2);
        setVoteCreatedAt(otherBotVote.getId(), LocalDateTime.now().minusDays(1));

        Member readerBot = saveMember("content-seed-bot-reader", true);

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(
                readerBot.getId(), Set.of(thisRunBot.getId(), readerBot.getId()));

        assertThat(result).extracting(CandidatePost::id)
                .containsExactly(humanVote.getId(), thisRunBotVote.getId(), otherBotVote.getId());
    }

    @Test
    @DisplayName("최근 최상위 댓글을 commentContextLimit개까지 마스킹해서 붙인다")
    void findInteractionCandidates_AttachesMaskedRecentTopLevelComments() {
        Member creator = saveMember("comment-context-creator", false);
        Vote vote = saveVoteWithOptions(creator, "comment-context", VoteCategory.FOOD, 2);
        setVoteCreatedAt(vote.getId(), LocalDateTime.now().minusDays(1));
        CommentGroup group = saveCommentGroup(vote);

        int extra = 2;
        int total = properties.getCommentContextLimit() + extra;
        Comment lastTopLevelComment = null;
        for (int i = 0; i < total; i++) {
            Member commenter = saveMember("comment-context-commenter-" + i, false);
            String content = i == total - 1 ? "연락처는 010-1234-5678 입니다" : "댓글 " + i;
            Comment comment = saveTopLevelComment(vote, group, commenter, content);
            setCommentCreatedAt(comment.getId(), LocalDateTime.now().minusMinutes(total - i));
            lastTopLevelComment = comment;
        }
        // 대댓글은 최상위 댓글 목록에 포함되지 않아야 한다.
        saveReplyComment(vote, group, creator, lastTopLevelComment, "대댓글");

        Member bot = saveMember("content-seed-bot-107", true);

        List<CandidatePost> result = candidateQueryService.findInteractionCandidates(bot.getId(), Set.of(bot.getId()));

        List<String> recentComments = result.get(0).recentTopComments();
        assertThat(recentComments).hasSize(properties.getCommentContextLimit());
        assertThat(recentComments.get(0)).isEqualTo("연락처는 [masked-phone] 입니다");
        assertThat(recentComments).doesNotContain("대댓글");
    }

    private Member saveMember(String socialId, boolean isBot) {
        return inTransaction(() -> memberRepository.save(Member.builder()
                .socialId(socialId)
                .email(socialId + "@example.com")
                .name(socialId)
                .nickname(socialId)
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .isBot(isBot)
                .build()));
    }

    private Vote saveVoteWithOptions(Member member, String title, VoteCategory category, int optionCount) {
        return inTransaction(() -> {
            Member managedMember = memberRepository.findById(member.getId()).orElseThrow();
            Vote vote = Vote.builder()
                    .member(managedMember)
                    .title(title)
                    .content("content")
                    .category(category)
                    .totalVoteCount(0)
                    .pinType(com.valanse.valanse.domain.enums.PinType.NONE)
                    .build();
            VoteLabel[] labels = VoteLabel.values();
            for (int i = 0; i < optionCount; i++) {
                vote.addVoteOption(VoteOption.builder()
                        .content("option-" + labels[i])
                        .label(labels[i])
                        .voteCount(0)
                        .build());
            }
            return voteRepository.saveAndFlush(vote);
        });
    }

    private void saveMemberVoteOption(Member member, Vote vote, VoteOption voteOption) {
        inTransaction(() -> memberVoteOptionRepository.saveAndFlush(MemberVoteOption.builder()
                .member(memberRepository.findById(member.getId()).orElseThrow())
                .vote(voteRepository.findById(vote.getId()).orElseThrow())
                .voteOption(voteOptionRepository.findById(voteOption.getId()).orElseThrow())
                .build()));
    }

    private CommentGroup saveCommentGroup(Vote vote) {
        return inTransaction(() -> {
            Vote managedVote = voteRepository.findById(vote.getId()).orElseThrow();
            return commentGroupRepository.saveAndFlush(CommentGroup.builder()
                    .vote(managedVote)
                    .totalCommentCount(0)
                    .build());
        });
    }

    private Comment saveTopLevelComment(Vote vote, CommentGroup group, Member member, String content) {
        return inTransaction(() -> commentRepository.saveAndFlush(Comment.builder()
                .content(content)
                .member(memberRepository.findById(member.getId()).orElseThrow())
                .commentGroup(commentGroupRepository.findById(group.getId()).orElseThrow())
                .likeCount(0)
                .replyCount(0)
                .build()));
    }

    private Comment saveReplyComment(Vote vote, CommentGroup group, Member member, Comment parent, String content) {
        return inTransaction(() -> commentRepository.saveAndFlush(Comment.builder()
                .content(content)
                .member(memberRepository.findById(member.getId()).orElseThrow())
                .commentGroup(commentGroupRepository.findById(group.getId()).orElseThrow())
                .parent(commentRepository.findById(parent.getId()).orElseThrow())
                .likeCount(0)
                .replyCount(0)
                .build()));
    }

    private void setVoteCreatedAt(Long voteId, LocalDateTime createdAt) {
        jdbcTemplate.update("update vote set created_at = ? where id = ?", createdAt, voteId);
    }

    private void setCommentCreatedAt(Long commentId, LocalDateTime createdAt) {
        jdbcTemplate.update("update comment set created_at = ? where id = ?", createdAt, commentId);
    }

    private void cleanUp() {
        inTransaction(() -> {
            memberVoteOptionRepository.deleteAllInBatch();
            commentRepository.deleteAllInBatch();
            commentGroupRepository.deleteAllInBatch();
            voteOptionRepository.deleteAllInBatch();
            voteRepository.deleteAllInBatch();
            memberProfileRepository.deleteAllInBatch();
            memberRepository.deleteAllInBatch();
            return null;
        });
    }

    private <T> T inTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }
}
