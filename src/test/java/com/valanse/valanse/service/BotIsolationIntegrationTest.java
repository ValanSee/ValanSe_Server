package com.valanse.valanse.service;

import com.valanse.valanse.domain.CommentGroup;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.VoteOption;
import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;
import com.valanse.valanse.domain.enums.MbtiIe;
import com.valanse.valanse.domain.enums.MbtiTf;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.domain.enums.VoteLabel;
import com.valanse.valanse.dto.Comment.CommentPostRequest;
import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;
import com.valanse.valanse.repository.CommentGroupRepository;
import com.valanse.valanse.repository.CommentRepository;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteResultQueryRepository;
import com.valanse.valanse.service.BotVoteService.BotVoteService;
import com.valanse.valanse.service.CommentService.CommentService;
import com.valanse.valanse.service.VoteService.VoteService;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 봇 계정의 투표/댓글 활동이 실제 사용자 지표(집계 카운트, 통계, 인기 순위, 포인트)를
 * 오염시키지 않는지 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class BotIsolationIntegrationTest {

    @Autowired private VoteService voteService;
    @Autowired private CommentService commentService;
    @Autowired private BotVoteService botVoteService;
    @Autowired private VoteResultQueryRepository voteResultQueryRepository;

    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
    @Autowired private CommentGroupRepository commentGroupRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PointHistoryRepository pointHistoryRepository;
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
    @DisplayName("봇 투표는 집계 카운트와 작성자 포인트에 영향을 주지 않는다")
    void botVote_DoesNotAffectCountsOrPoints() {
        Member creator = saveMember("real-creator", false);
        saveProfile(creator, Gender.FEMALE, Age.TWENTY);
        Member bot = saveMember("content-seed-bot-001", true);
        saveProfile(bot, Gender.MALE, Age.THIRTY);

        Vote vote = saveVoteWithOptions(creator);
        VoteOption option = vote.getVoteOptions().get(0);

        botVoteService.castBotVote(bot.getId(), option.getId());

        Vote reloadedVote = voteRepository.findById(vote.getId()).orElseThrow();
        VoteOption reloadedOption = voteOptionRepository.findById(option.getId()).orElseThrow();

        assertThat(reloadedVote.getTotalVoteCount()).isZero();
        assertThat(reloadedOption.getVoteCount()).isZero();
        assertThat(memberVoteOptionRepository.findByMemberIdAndVoteId(bot.getId(), vote.getId())).isPresent();
        assertThat(pointHistoryRepository.findByMemberId(creator.getId())).isEmpty();
    }

    @Test
    @DisplayName("성별 통계는 봇 투표를 제외한다")
    void genderStats_ExcludeBotVotes() {
        Member creator = saveMember("real-creator2", false);
        saveProfile(creator, Gender.MALE, Age.TWENTY);
        Vote vote = saveVoteWithOptions(creator);
        VoteOption option = vote.getVoteOptions().get(0);

        Member realVoter = saveMember("real-voter", false);
        saveProfile(realVoter, Gender.FEMALE, Age.TWENTY);
        voteService.processVote(realVoter.getId(), vote.getId(), option.getId());

        Member bot = saveMember("content-seed-bot-002", true);
        saveProfile(bot, Gender.FEMALE, Age.THIRTY);
        botVoteService.castBotVote(bot.getId(), option.getId());

        VoteGenderResultResponseDto result = voteResultQueryRepository.findVoteResultByGender(vote.getId(), "FEMALE");

        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("연령 통계는 봇 투표를 제외한다")
    void ageStats_ExcludeBotVotes() {
        Member creator = saveMember("age-stats-creator", false);
        saveProfile(creator, Gender.MALE, Age.TWENTY);
        Vote vote = saveVoteWithOptions(creator);
        VoteOption option = vote.getVoteOptions().get(0);

        Member realVoter = saveMember("age-stats-real-voter", false);
        saveProfile(realVoter, Gender.FEMALE, Age.TWENTY);
        voteService.processVote(realVoter.getId(), vote.getId(), option.getId());

        Member bot = saveMember("content-seed-bot-006", true);
        saveProfile(bot, Gender.FEMALE, Age.TWENTY);
        botVoteService.castBotVote(bot.getId(), option.getId());

        VoteAgeResultResponseDto result = voteResultQueryRepository.findVoteResultByAge(vote.getId());

        VoteAgeResultResponseDto.AgeGroupStats stats = result.getAgeRatios().get(option.getLabel().name());
        assertThat(stats.getTotalCount()).isEqualTo(1);
        assertThat(stats.getAgeGroups().get("20대").getVoteCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("MBTI 통계는 봇 투표를 제외한다")
    void mbtiStats_ExcludeBotVotes() {
        Member creator = saveMember("mbti-stats-creator", false);
        saveProfile(creator, Gender.MALE, Age.TWENTY);
        Vote vote = saveVoteWithOptions(creator);
        VoteOption option = vote.getVoteOptions().get(0);

        Member realVoter = saveMember("mbti-stats-real-voter", false);
        saveProfileWithMbti(realVoter, Gender.FEMALE, Age.TWENTY, MbtiIe.I, MbtiTf.F, "INFP");
        voteService.processVote(realVoter.getId(), vote.getId(), option.getId());

        Member bot = saveMember("content-seed-bot-007", true);
        saveProfileWithMbti(bot, Gender.FEMALE, Age.TWENTY, MbtiIe.I, MbtiTf.F, "INFP");
        botVoteService.castBotVote(bot.getId(), option.getId());

        VoteMbtiResultResponseDto result = voteResultQueryRepository.findVoteResultByMbti(vote.getId(), "ie");

        VoteMbtiResultResponseDto.OptionRatio optionResult = result.getMbti_ratios().get("I").stream()
                .filter(r -> r.getContent().equals(option.getContent()))
                .findFirst()
                .orElseThrow();
        assertThat(optionResult.getVote_count()).isEqualTo(1);
    }

    @Test
    @DisplayName("트렌딩 순위는 봇 투표·댓글만 있는 게시글을 후보에서 제외한다")
    void trendingRanking_ExcludesBotOnlyActivity() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);

        Member realVoteCreator = saveMember("trending-real-participation", false);
        saveProfile(realVoteCreator, Gender.MALE, Age.TWENTY);
        Vote voteWithRealVote = saveVoteWithOptions(realVoteCreator);
        VoteOption realOption = voteWithRealVote.getVoteOptions().get(0);

        Member realVoter = saveMember("trending-real-voter", false);
        saveProfile(realVoter, Gender.MALE, Age.TWENTY);
        voteService.processVote(realVoter.getId(), voteWithRealVote.getId(), realOption.getId());

        Member botOnlyCreator = saveMember("trending-bot-only", false);
        saveProfile(botOnlyCreator, Gender.MALE, Age.TWENTY);
        Vote voteWithBotActivity = saveVoteWithOptions(botOnlyCreator);
        VoteOption botOption = voteWithBotActivity.getVoteOptions().get(0);

        Member bot = saveMember("content-seed-bot-008", true);
        saveProfile(bot, Gender.FEMALE, Age.TWENTY);
        botVoteService.castBotVote(bot.getId(), botOption.getId());
        commentService.createComment(voteWithBotActivity.getId(), bot.getId(),
                CommentPostRequest.builder().content("bot comment").build());

        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        Vote trendingVote = voteRepository.findTrendingVote(from, to).orElseThrow();
        assertThat(trendingVote.getId()).isEqualTo(voteWithRealVote.getId());
    }

    @Test
    @DisplayName("핫이슈 순위는 봇 댓글을 반응성 점수에서 제외하지만 화면 노출용 댓글 수는 그대로 유지한다")
    void hotIssueRanking_ExcludesBotCommentsFromScore() {
        Member realVoteCreator = saveMember("vote-with-real-participation", false);
        saveProfile(realVoteCreator, Gender.MALE, Age.TWENTY);
        Vote voteWithRealVote = saveVoteWithOptions(realVoteCreator);
        VoteOption realOption = voteWithRealVote.getVoteOptions().get(0);

        Member realVoter = saveMember("hot-real-voter", false);
        saveProfile(realVoter, Gender.MALE, Age.TWENTY);
        voteService.processVote(realVoter.getId(), voteWithRealVote.getId(), realOption.getId());

        Member botOnlyCreator = saveMember("vote-with-only-bot-comments", false);
        saveProfile(botOnlyCreator, Gender.MALE, Age.TWENTY);
        Vote voteWithBotComments = saveVoteWithOptions(botOnlyCreator);
        CommentGroup group = saveCommentGroup(voteWithBotComments);

        Member bot = saveMember("content-seed-bot-003", true);
        saveProfile(bot, Gender.FEMALE, Age.TWENTY);
        for (int i = 0; i < 5; i++) {
            commentService.createComment(voteWithBotComments.getId(), bot.getId(),
                    CommentPostRequest.builder().content("bot comment " + i).build());
        }

        CommentGroup reloadedGroup = commentGroupRepository.findById(group.getId()).orElseThrow();
        assertThat(reloadedGroup.getTotalCommentCount()).isEqualTo(5); // 화면 노출용 카운트는 봇 댓글도 그대로 누적

        Vote hotIssueVote = voteRepository.findHotIssueVote().orElseThrow();
        assertThat(hotIssueVote.getId()).isEqualTo(voteWithRealVote.getId());
    }

    @Test
    @DisplayName("탈퇴 회원의 댓글이 익명화(member_id=NULL)되어도 반응성 점수에서 제외되지 않는다")
    void hotIssueRanking_IncludesAnonymizedMemberComments() {
        Member voteCreator = saveMember("vote-with-anonymized-comment", false);
        saveProfile(voteCreator, Gender.MALE, Age.TWENTY);
        Vote voteWithAnonymizedComment = saveVoteWithOptions(voteCreator);

        Member withdrawnMember = saveMember("withdrawn-commenter", false);
        saveProfile(withdrawnMember, Gender.FEMALE, Age.TWENTY);
        Long commentId = commentService.createComment(voteWithAnonymizedComment.getId(), withdrawnMember.getId(),
                CommentPostRequest.builder().content("탈퇴 전에 남긴 실제 댓글").build());

        // 회원 물리 삭제 시 SoftDeletePurgeService.anonymizeComments()가 하는 것과 동일하게
        // 댓글 작성자를 NULL로 익명화한 상황을 재현합니다.
        inTransaction(() -> {
            jdbcTemplate.update("update comment set member_id = null where id = ?", commentId);
            return null;
        });

        Member laterCreator = saveMember("vote-with-no-activity", false);
        saveProfile(laterCreator, Gender.MALE, Age.TWENTY);
        saveVoteWithOptions(laterCreator); // 활동이 전혀 없는, 더 나중에 생성된 투표 (비교 대상)

        Vote hotIssueVote = voteRepository.findHotIssueVote().orElseThrow();
        assertThat(hotIssueVote.getId()).isEqualTo(voteWithAnonymizedComment.getId());
    }

    @Test
    @DisplayName("트렌딩 조회도 탈퇴 회원의 익명화된 댓글을 기간 내 활동으로 인정한다")
    void trendingVote_IncludesAnonymizedMemberComments() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);

        Member voteCreator = saveMember("trending-vote-with-anonymized-comment", false);
        saveProfile(voteCreator, Gender.MALE, Age.TWENTY);
        Vote voteWithAnonymizedComment = saveVoteWithOptions(voteCreator);

        Member withdrawnMember = saveMember("trending-withdrawn-commenter", false);
        saveProfile(withdrawnMember, Gender.FEMALE, Age.TWENTY);
        Long commentId = commentService.createComment(voteWithAnonymizedComment.getId(), withdrawnMember.getId(),
                CommentPostRequest.builder().content("탈퇴 전에 남긴 실제 댓글").build());

        // 회원 물리 삭제 시 SoftDeletePurgeService.anonymizeComments()가 하는 것과 동일하게
        // 댓글 작성자를 NULL로 익명화한 상황을 재현합니다.
        inTransaction(() -> {
            jdbcTemplate.update("update comment set member_id = null where id = ?", commentId);
            return null;
        });

        LocalDateTime to = LocalDateTime.now().plusMinutes(1);

        // 익명화된 댓글이 "기간 내 활동 존재" 조건에서 봇 댓글처럼 배제되면 이 투표는 후보에서
        // 아예 빠져 findTrendingVote가 빈 결과를 반환한다.
        Vote trendingVote = voteRepository.findTrendingVote(from, to).orElseThrow();
        assertThat(trendingVote.getId()).isEqualTo(voteWithAnonymizedComment.getId());
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

    private MemberProfile saveProfile(Member member, Gender gender, Age age) {
        return inTransaction(() -> {
            Member managedMember = memberRepository.findById(member.getId()).orElseThrow();
            return memberProfileRepository.save(MemberProfile.builder()
                    .member(managedMember)
                    .nickname(managedMember.getNickname())
                    .gender(gender)
                    .age(age)
                    .point(0L)
                    .build());
        });
    }

    private MemberProfile saveProfileWithMbti(Member member, Gender gender, Age age, MbtiIe mbtiIe, MbtiTf mbtiTf, String mbti) {
        return inTransaction(() -> {
            Member managedMember = memberRepository.findById(member.getId()).orElseThrow();
            return memberProfileRepository.save(MemberProfile.builder()
                    .member(managedMember)
                    .nickname(managedMember.getNickname())
                    .gender(gender)
                    .age(age)
                    .mbtiIe(mbtiIe)
                    .mbtiTf(mbtiTf)
                    .mbti(mbti)
                    .point(0L)
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

    private CommentGroup saveCommentGroup(Vote vote) {
        return inTransaction(() -> {
            Vote managedVote = voteRepository.findById(vote.getId()).orElseThrow();
            return commentGroupRepository.saveAndFlush(CommentGroup.builder()
                    .vote(managedVote)
                    .totalCommentCount(0)
                    .build());
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
            memberProfileRepository.deleteAllInBatch();
            memberRepository.deleteAllInBatch();
            return null;
        });
    }

    private <T> T inTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }
}
