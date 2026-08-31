package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR3-3 "항목별 트랜잭션 저장"의 규칙(별도 persistence bean의 REQUIRES_NEW,
 * 기존 VoteService·CommentService 재사용, 일부 실패 시 앞선 성공 유지)이 실제로
 * 지켜지는지 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContentSeedPersistenceServiceTest {

    @Autowired private ContentSeedPersistenceService persistenceService;

    @Autowired private MemberRepository memberRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PointHistoryRepository pointHistoryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUpBefore() {
        cleanUp();
    }

    @AfterEach
    void cleanUpAfter() {
        cleanUp();
    }

    @Test
    @DisplayName("봇 게시글 1건을 저장하고, 봇에게는 작성 포인트를 지급하지 않는다")
    void saveBotPost_PersistsVoteWithoutRewardingBot() {
        Member bot = saveMember("content-seed-bot-301", true);
        GeneratedPost post = new GeneratedPost("제목입니다", "본문입니다", "A를 고른다", "B를 고른다", GeneratableVoteCategory.FOOD);

        Long voteId = persistenceService.saveBotPost(bot.getId(), post);

        inTransaction(() -> {
            Vote saved = voteRepository.findById(voteId).orElseThrow();
            assertThat(saved.getTitle()).isEqualTo("제목입니다");
            assertThat(saved.getCategory()).isEqualTo(VoteCategory.FOOD);
            assertThat(saved.getVoteOptions()).hasSize(2);
            return null;
        });
        assertThat(commentGroupRepository.findByVoteId(voteId)).isPresent();
        assertThat(pointHistoryRepository.findByMemberId(bot.getId())).isEmpty();
    }

    @Test
    @DisplayName("봇 상호작용(투표+댓글) 1건을 저장하고, 투표는 공개 카운트를 늘리지 않는다")
    void saveBotInteraction_PersistsVoteAndCommentWithoutPublicVoteCount() {
        Member author = saveMember("interaction-author", false);
        Vote vote = saveVoteWithOptions(author, "interaction-title");
        VoteOption optionA = vote.getVoteOptions().get(0);

        Member bot = saveMember("content-seed-bot-302", true);
        GeneratedInteraction interaction = new GeneratedInteraction(
                vote.getId(), GeneratedInteraction.SelectedOption.A, "저는 A가 좋아요");

        Long commentId = persistenceService.saveBotInteraction(bot.getId(), interaction);

        assertThat(memberVoteOptionRepository.findByMemberIdAndVoteId(bot.getId(), vote.getId())).isPresent();
        assertThat(commentRepository.findById(commentId)).isPresent();
        assertThat(commentRepository.findById(commentId).orElseThrow().getContent()).isEqualTo("저는 A가 좋아요");

        Vote reloadedVote = voteRepository.findById(vote.getId()).orElseThrow();
        assertThat(reloadedVote.getTotalVoteCount()).isZero();
        VoteOption reloadedOption = voteOptionRepository.findById(optionA.getId()).orElseThrow();
        assertThat(reloadedOption.getVoteCount()).isZero();
    }

    @Test
    @DisplayName("REQUIRES_NEW 저장은 이를 감싼 바깥 트랜잭션이 나중에 롤백되어도 유지된다")
    void saveBotPost_CommitsIndependentlyOfSurroundingTransactionRollback() {
        Member bot = saveMember("content-seed-bot-303", true);
        GeneratedPost post = new GeneratedPost("독립커밋제목", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        long[] savedVoteId = new long[1];

        Assertions.assertThrows(RuntimeException.class, () ->
                new TransactionTemplate(transactionManager).execute(status -> {
                    savedVoteId[0] = persistenceService.saveBotPost(bot.getId(), post);
                    throw new RuntimeException("바깥 트랜잭션 강제 실패");
                })
        );

        assertThat(voteRepository.findById(savedVoteId[0])).isPresent();
    }

    @Test
    @DisplayName("뒤 항목 저장이 실패해도 앞서 저장된 항목은 그대로 남는다")
    void saveBotInteraction_FailureDoesNotRollBackEarlierSuccess() {
        Member bot = saveMember("content-seed-bot-304", true);
        GeneratedPost post = new GeneratedPost("먼저성공제목", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        Long voteId = persistenceService.saveBotPost(bot.getId(), post);

        GeneratedInteraction badInteraction = new GeneratedInteraction(
                -1L, GeneratedInteraction.SelectedOption.A, "존재하지 않는 게시글");

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                persistenceService.saveBotInteraction(bot.getId(), badInteraction));

        assertThat(voteRepository.findById(voteId)).isPresent();
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

    private Vote saveVoteWithOptions(Member member, String title) {
        return inTransaction(() -> {
            Member managedMember = memberRepository.findById(member.getId()).orElseThrow();
            Vote vote = Vote.builder()
                    .member(managedMember)
                    .title(title)
                    .content("content")
                    .category(VoteCategory.FOOD)
                    .totalVoteCount(0)
                    .pinType(PinType.NONE)
                    .build();
            vote.addVoteOption(VoteOption.builder().content("option-A").label(VoteLabel.A).voteCount(0).build());
            vote.addVoteOption(VoteOption.builder().content("option-B").label(VoteLabel.B).voteCount(0).build());
            return voteRepository.saveAndFlush(vote);
        });
    }

    private void cleanUp() {
        inTransaction(() -> {
            memberVoteOptionRepository.deleteAllInBatch();
            pointHistoryRepository.deleteAllInBatch();
            commentRepository.deleteAllInBatch();
            commentGroupRepository.deleteAllInBatch();
            voteOptionRepository.deleteAllInBatch();
            voteRepository.deleteAllInBatch();
            memberRepository.deleteAllInBatch();
            return null;
        });
    }

    private <T> T inTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }
}
