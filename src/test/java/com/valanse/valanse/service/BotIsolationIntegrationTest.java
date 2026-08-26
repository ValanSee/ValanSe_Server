package com.valanse.valanse.service;

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
import com.valanse.valanse.dto.VotesCheck.VoteAgeResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteGenderResultResponseDto;
import com.valanse.valanse.dto.VotesCheck.VoteMbtiResultResponseDto;
import com.valanse.valanse.repository.MemberProfileRepository;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.repository.MemberVoteOptionRepository;
import com.valanse.valanse.repository.PointHistoryRepository;
import com.valanse.valanse.repository.VoteOptionRepository;
import com.valanse.valanse.repository.VoteRepository;
import com.valanse.valanse.repository.VotesCheckRepositoryCustom.VoteResultQueryRepository;
import com.valanse.valanse.service.VoteService.VoteService;
import org.junit.jupiter.api.AfterEach;
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
 * 봇 계정의 투표 활동이 성별·연령·MBTI 통계를 오염시키지 않는지 검증하는 통합 테스트입니다.
 * 트렌딩·핫이슈 랭킹 제외 검증은 PR1 항목 5(인기·트렌딩)에서 이 파일에 추가됩니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class BotIsolationIntegrationTest {

    @Autowired private VoteService voteService;
    @Autowired private VoteResultQueryRepository voteResultQueryRepository;

    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberProfileRepository memberProfileRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private MemberVoteOptionRepository memberVoteOptionRepository;
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
        voteService.processVote(bot.getId(), vote.getId(), option.getId());

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
        voteService.processVote(bot.getId(), vote.getId(), option.getId());

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
        voteService.processVote(bot.getId(), vote.getId(), option.getId());

        VoteMbtiResultResponseDto result = voteResultQueryRepository.findVoteResultByMbti(vote.getId(), "ie");

        VoteMbtiResultResponseDto.OptionRatio optionResult = result.getMbti_ratios().get("I").stream()
                .filter(r -> r.getContent().equals(option.getContent()))
                .findFirst()
                .orElseThrow();
        assertThat(optionResult.getVote_count()).isEqualTo(1);
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

    private void cleanUp() {
        inTransaction(() -> {
            memberVoteOptionRepository.deleteAllInBatch();
            pointHistoryRepository.deleteAllInBatch();
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
