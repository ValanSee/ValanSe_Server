package com.valanse.valanse.service;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Comment;
import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.MemberProfileTitle;
import com.valanse.valanse.domain.PointHistory;
import com.valanse.valanse.domain.Title;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.domain.mapping.CommentLike;
import com.valanse.valanse.domain.mapping.MemberVoteOption;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentLikeRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberProfileTitleRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import com.valanse.valanse.repository.TitleRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.service.CommentLikeService.CommentLikeService;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.PointService.PointService;
import com.valanse.valanse.service.TitleService.TitleService;
import com.valanse.valanse.service.VoteService.VoteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIntegrityTest {

    @Autowired private VoteService voteService;
    @Autowired private CommentService commentService;
    @Autowired private CommentLikeService commentLikeService;
    @Autowired private PointService pointService;
    @Autowired private TitleService titleService;

    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private CommentLikeRepository commentLikeRepository;
    @Autowired private PointHistoryRepository pointHistoryRepository;
    @Autowired private TitleRepository titleRepository;
    @Autowired private MemberProfileTitleRepository memberProfileTitleRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUpBefore() {
        cleanUp();
    }

    @AfterEach
    void cleanUpAfter() {
        SecurityContextHolder.clearContext();
        cleanUp();
    }

    @Test
    @DisplayName("MemberVoteOption, CommentLike, MemberProfileTitle unique 제약이 중복 저장을 막는다")
    void uniqueConstraintsPreventDuplicateRows() {
        Member member = saveMember("unique-user");
        MemberProfile profile = saveProfile(member, 0L);
        Vote vote = saveVoteWithOptions(member);
        VoteOption option = vote.getVoteOptions().get(0);
        CommentGroup group = saveCommentGroup(vote, 1);
        Comment comment = saveComment(member, group, null, 0);
        Title title = saveTitle("UNIQUE_TITLE", 100L);

        saveMemberVoteOption(member.getId(), vote.getId(), option.getId());
        assertThatThrownBy(() -> saveMemberVoteOption(member.getId(), vote.getId(), option.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        saveCommentLike(member.getId(), comment.getId());
        assertThatThrownBy(() -> saveCommentLike(member.getId(), comment.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        saveMemberProfileTitle(profile.getId(), title.getId());
        assertThatThrownBy(() -> saveMemberProfileTitle(profile.getId(), title.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 사용자의 동시 투표 요청은 row 중복 없이 카운트와 실제 row 수를 일치시킨다")
    void concurrentVoteRequestsKeepRowsAndCountsConsistent() throws Exception {
        Member member = saveMember("vote-user");
        saveProfile(member, 0L);
        Vote vote = saveVoteWithOptions(member);
        VoteOption option = vote.getVoteOptions().get(0);

        runConcurrently(5, () -> {
            voteService.processVote(member.getId(), vote.getId(), option.getId());
            return null;
        });

        long voteRows = memberVoteOptionRepository.findAll().stream()
                .filter(row -> row.getMember().getId().equals(member.getId()))
                .filter(row -> row.getVote().getId().equals(vote.getId()))
                .count();
        Vote reloadedVote = voteRepository.findById(vote.getId()).orElseThrow();
        VoteOption reloadedOption = voteOptionRepository.findById(option.getId()).orElseThrow();

        assertThat(voteRows).isLessThanOrEqualTo(1);
        assertThat(reloadedVote.getTotalVoteCount()).isEqualTo((int) voteRows);
        assertThat(reloadedOption.getVoteCount()).isEqualTo((int) voteRows);
        assertThat(reloadedVote.getTotalVoteCount()).isGreaterThanOrEqualTo(0);
        assertThat(reloadedOption.getVoteCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("댓글 중복 삭제는 totalCommentCount와 replyCount를 음수로 만들지 않는다")
    void duplicateCommentDeleteDoesNotMakeCountsNegative() throws Exception {
        Member member = saveMember("comment-delete-user");
        Vote vote = saveVoteWithOptions(member);
        CommentGroup group = saveCommentGroup(vote, 1);
        Comment parent = saveComment(member, group, null, 0);

        runConcurrently(2, () -> {
            commentService.deleteMyComment(member, parent.getId());
            return null;
        });

        CommentGroup reloadedGroup = commentGroupRepository.findById(group.getId()).orElseThrow();
        assertThat(reloadedGroup.getTotalCommentCount()).isZero();

        Comment replyParent = saveComment(member, group, null, 1);
        Comment reply = saveComment(member, group, replyParent, 0);
        runConcurrently(2, () -> {
            commentService.deleteMyComment(member, reply.getId());
            return null;
        });

        Comment reloadedParent = commentRepository.findById(replyParent.getId()).orElseThrow();
        assertThat(reloadedParent.getReplyCount()).isZero();
    }

    @Test
    @DisplayName("같은 사용자의 동시 좋아요 요청은 row 중복 없이 likeCount와 실제 row 수를 일치시킨다")
    void concurrentLikeRequestsKeepRowsAndCountsConsistent() throws Exception {
        Member member = saveMember("like-user");
        Vote vote = saveVoteWithOptions(member);
        CommentGroup group = saveCommentGroup(vote, 1);
        Comment comment = saveComment(member, group, null, 0);

        runConcurrently(5, () -> {
            setAuthentication(member.getId());
            commentLikeService.likeComment(vote.getId(), comment.getId());
            SecurityContextHolder.clearContext();
            return null;
        });

        long likeRows = commentLikeRepository.findAll().stream()
                .filter(row -> row.getUser().getId().equals(member.getId()))
                .filter(row -> row.getComment().getId().equals(comment.getId()))
                .count();
        Comment reloadedComment = commentRepository.findById(comment.getId()).orElseThrow();

        assertThat(likeRows).isLessThanOrEqualTo(1);
        assertThat(reloadedComment.getLikeCount()).isEqualTo((int) likeRows);
        assertThat(reloadedComment.getLikeCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("댓글 작성 포인트 일일 제한은 동시 요청에서도 초과 지급되지 않는다")
    void concurrentCommentCreatePointDailyLimitIsNotExceeded() throws Exception {
        Member member = saveMember("point-user");
        saveProfile(member, 0L);

        runConcurrently(10, () -> {
            pointService.givePoint(member.getId(), PointType.COMMENT_CREATE);
            return null;
        });

        MemberProfile profile = memberProfileRepository.findByMemberId(member.getId()).orElseThrow();
        long commentPointHistories = pointHistoryRepository.findByMemberId(member.getId()).stream()
                .filter(history -> history.getType() == PointType.COMMENT_CREATE)
                .count();

        assertThat(commentPointHistories).isEqualTo(3);
        assertThat(profile.getPoint()).isEqualTo(3L);
    }

    @Test
    @DisplayName("칭호 동시 구매는 중복 소유와 중복 차감을 만들지 않는다")
    void concurrentTitlePurchaseDoesNotDuplicateOwnershipOrCharge() throws Exception {
        Member member = saveMember("title-user");
        saveProfile(member, 500L);
        Title title = saveTitle("POINT_TITLE", 300L);

        runConcurrently(5, () -> {
            try {
                titleService.purchaseTitle(member.getId(), title.getId());
            } catch (ApiException ignored) {
            }
            return null;
        });

        MemberProfile profile = memberProfileRepository.findByMemberId(member.getId()).orElseThrow();
        long ownedRows = memberProfileTitleRepository.findAll().stream()
                .filter(row -> row.getMemberProfile().getId().equals(profile.getId()))
                .filter(row -> row.getTitle().getId().equals(title.getId()))
                .count();
        long purchaseHistories = pointHistoryRepository.findByMemberId(member.getId()).stream()
                .filter(history -> history.getType() == PointType.TITLE_PURCHASE)
                .filter(history -> history.getAmount().equals(-300L))
                .count();

        assertThat(ownedRows).isEqualTo(1);
        assertThat(profile.getPoint()).isEqualTo(200L);
        assertThat(purchaseHistories).isEqualTo(1);
    }

    @Test
    @DisplayName("대댓글 동시 작성은 replyCount를 실제 대댓글 수와 일치시킨다")
    void concurrentReplyCreateKeepsReplyCountConsistent() throws Exception {
        Member member = saveMember("reply-user");
        Vote vote = saveVoteWithOptions(member);
        CommentGroup group = saveCommentGroup(vote, 1);
        Comment parent = saveComment(member, group, null, 0);

        runConcurrently(8, () -> {
            commentService.createComment(vote.getId(), member.getId(), CommentPostRequest.builder()
                    .content("reply")
                    .parentId(parent.getId())
                    .build());
            return null;
        });

        Comment reloadedParent = commentRepository.findById(parent.getId()).orElseThrow();
        long replyRows = commentRepository.findAll().stream()
                .filter(comment -> comment.getParent() != null)
                .filter(comment -> comment.getParent().getId().equals(parent.getId()))
                .filter(comment -> comment.getDeletedAt() == null)
                .count();

        assertThat(reloadedParent.getReplyCount()).isEqualTo((int) replyRows);
        assertThat(reloadedParent.getReplyCount()).isEqualTo(8);
    }

    private void runConcurrently(int times, Callable<Void> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(times);
        CountDownLatch ready = new CountDownLatch(times);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < times; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return task.call();
            }));
        }

        ready.await();
        start.countDown();
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdownNow();
    }

    private Member saveMember(String socialId) {
        return inTransaction(() -> memberRepository.save(Member.builder()
                .socialId(socialId)
                .email(socialId + "@example.com")
                .name(socialId)
                .nickname(socialId)
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .build()));
    }

    private MemberProfile saveProfile(Member member, long point) {
        return inTransaction(() -> {
            Member managedMember = memberRepository.findById(member.getId()).orElseThrow();
            return memberProfileRepository.save(MemberProfile.builder()
                    .member(managedMember)
                    .nickname(managedMember.getNickname())
                    .point(point)
                    .build());
        });
    }

    private Vote saveVoteWithOptions(Member member) {
        return inTransaction(() -> {
            Member managedMember = memberRepository.findById(member.getId()).orElseThrow();
            Vote vote = Vote.builder()
                    .member(managedMember)
                    .title("vote")
                    .content("content")
                    .category(VoteCategory.ALL)
                    .totalVoteCount(0)
                    .pinType(PinType.NONE)
                    .build();
            vote.addVoteOption(VoteOption.builder()
                    .content("A")
                    .label(VoteLabel.A)
                    .voteCount(0)
                    .build());
            vote.addVoteOption(VoteOption.builder()
                    .content("B")
                    .label(VoteLabel.B)
                    .voteCount(0)
                    .build());
            return voteRepository.saveAndFlush(vote);
        });
    }

    private CommentGroup saveCommentGroup(Vote vote, int totalCommentCount) {
        return inTransaction(() -> {
            Vote managedVote = voteRepository.findById(vote.getId()).orElseThrow();
            return commentGroupRepository.saveAndFlush(CommentGroup.builder()
                    .vote(managedVote)
                    .totalCommentCount(totalCommentCount)
                    .build());
        });
    }

    private Comment saveComment(Member member, CommentGroup group, Comment parent, int replyCount) {
        return inTransaction(() -> {
            Member managedMember = memberRepository.findById(member.getId()).orElseThrow();
            CommentGroup managedGroup = commentGroupRepository.findById(group.getId()).orElseThrow();
            Comment managedParent = parent == null ? null : commentRepository.findById(parent.getId()).orElseThrow();
            return commentRepository.saveAndFlush(Comment.builder()
                    .content("comment")
                    .member(managedMember)
                    .commentGroup(managedGroup)
                    .parent(managedParent)
                    .likeCount(0)
                    .replyCount(replyCount)
                    .build());
        });
    }

    private Title saveTitle(String code, long price) {
        return inTransaction(() -> titleRepository.saveAndFlush(Title.builder()
                .code(code)
                .name(code)
                .tier(TitleTier.BASIC)
                .acquisitionType(TitleAcquisitionType.POINT_PURCHASE)
                .price(price)
                .active(true)
                .build()));
    }

    private void saveMemberVoteOption(Long memberId, Long voteId, Long optionId) {
        inTransaction(() -> {
            memberVoteOptionRepository.saveAndFlush(MemberVoteOption.builder()
                    .member(memberRepository.findById(memberId).orElseThrow())
                    .vote(voteRepository.findById(voteId).orElseThrow())
                    .voteOption(voteOptionRepository.findById(optionId).orElseThrow())
                    .build());
            return null;
        });
    }

    private void saveCommentLike(Long memberId, Long commentId) {
        inTransaction(() -> {
            commentLikeRepository.saveAndFlush(CommentLike.builder()
                    .user(memberRepository.findById(memberId).orElseThrow())
                    .comment(commentRepository.findById(commentId).orElseThrow())
                    .build());
            return null;
        });
    }

    private void saveMemberProfileTitle(Long profileId, Long titleId) {
        inTransaction(() -> {
            memberProfileTitleRepository.saveAndFlush(MemberProfileTitle.builder()
                    .memberProfile(memberProfileRepository.findById(profileId).orElseThrow())
                    .title(titleRepository.findById(titleId).orElseThrow())
                    .build());
            return null;
        });
    }

    private void setAuthentication(Long memberId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId.toString(), "")
        );
    }

    private void cleanUp() {
        inTransaction(() -> {
            commentLikeRepository.deleteAllInBatch();
            memberVoteOptionRepository.deleteAllInBatch();
            memberProfileTitleRepository.deleteAllInBatch();
            pointHistoryRepository.deleteAllInBatch();
            commentRepository.deleteAllInBatch();
            commentGroupRepository.deleteAllInBatch();
            voteOptionRepository.deleteAllInBatch();
            voteRepository.deleteAllInBatch();
            titleRepository.deleteAllInBatch();
            memberProfileRepository.deleteAllInBatch();
            memberRepository.deleteAllInBatch();
            return null;
        });
    }

    private <T> T inTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }
}
