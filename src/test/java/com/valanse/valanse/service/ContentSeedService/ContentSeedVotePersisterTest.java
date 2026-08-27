package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.dto.Comment.CommentResponseDto;
import com.valanse.valanse.dto.Comment.PagedCommentResponse;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.service.CommentService.CommentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR3-2 "부작용 없는 봇 투표 저장"의 규칙(MemberVoteOption만 저장, 공개 카운트
 * 미증가, 포인트 미지급, 댓글 선택 옵션 표시 유지)이 실제로 지켜지는지 검증하는
 * 통합 테스트입니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContentSeedVotePersisterTest {

    @Autowired private ContentSeedVotePersister votePersister;
    @Autowired private CommentService commentService;

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
    @DisplayName("MemberVoteOption 관계만 저장하고 공개 투표 카운트는 증가시키지 않는다")
    void saveVote_SavesMemberVoteOptionWithoutIncrementingPublicCounts() {
        Member author = saveMember("vote-persist-author", false);
        Vote vote = saveVoteWithOptions(author, "vote-persist-title");
        VoteOption optionA = vote.getVoteOptions().get(0);

        Member bot = saveMember("content-seed-bot-201", true);

        votePersister.saveVote(bot, vote, optionA);

        Optional<com.valanse.valanse.domain.mapping.MemberVoteOption> saved =
                memberVoteOptionRepository.findByMemberIdAndVoteId(bot.getId(), vote.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getVoteOption().getId()).isEqualTo(optionA.getId());

        Vote reloadedVote = voteRepository.findById(vote.getId()).orElseThrow();
        assertThat(reloadedVote.getTotalVoteCount()).isZero();
        VoteOption reloadedOption = voteOptionRepository.findById(optionA.getId()).orElseThrow();
        assertThat(reloadedOption.getVoteCount()).isZero();
    }

    @Test
    @DisplayName("게시글 작성자와 투표한 봇 모두 포인트를 받지 않는다")
    void saveVote_GivesNoPointsToAuthorOrBot() {
        Member author = saveMember("vote-persist-point-author", false);
        Vote vote = saveVoteWithOptions(author, "vote-persist-point-title");
        VoteOption optionA = vote.getVoteOptions().get(0);

        Member bot = saveMember("content-seed-bot-202", true);

        votePersister.saveVote(bot, vote, optionA);

        assertThat(pointHistoryRepository.findByMemberId(author.getId())).isEmpty();
        assertThat(pointHistoryRepository.findByMemberId(bot.getId())).isEmpty();
    }

    @Test
    @DisplayName("봇 투표 후 남긴 댓글에도 선택한 옵션 라벨이 표시된다")
    void saveVote_ThenComment_ShowsSelectedOptionLabel() {
        Member author = saveMember("vote-persist-comment-author", false);
        Vote vote = saveVoteWithOptions(author, "vote-persist-comment-title");
        VoteOption optionA = vote.getVoteOptions().get(0);
        assertThat(optionA.getLabel()).isEqualTo(VoteLabel.A);

        Member bot = saveMember("content-seed-bot-203", true);
        votePersister.saveVote(bot, vote, optionA);

        Long commentId = commentService.createComment(vote.getId(), bot.getId(),
                CommentPostRequest.builder().content("A가 더 나아요").build());

        PagedCommentResponse response =
                commentService.getCommentsByVoteId(vote.getId(), "latest", PageRequest.of(0, 10), null, false);
        CommentResponseDto commentDto = response.getComments().stream()
                .filter(c -> c.getCommentId().equals(commentId))
                .findFirst()
                .orElseThrow();

        assertThat(commentDto.getVoteOptionLabel()).isEqualTo("A");
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
