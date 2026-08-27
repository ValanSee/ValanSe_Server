package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 주간 콘텐츠 시드 생성 전체 흐름(봇 선정 -> 전체 게시글 생성 -> 전체 상호작용 생성)을
// 오케스트레이션한다. 실패 격리 원칙:
// - 항목 단위: 품질 미달이거나 저장에 실패한 항목 하나가 나머지 항목에 영향을 주지 않는다.
// - 배치 단위: 봇 한 명의 Claude 호출 자체가 실패해도(예외) 그 봇의 실패로만 기록하고 계속한다.
// - 봇 단위: 봇 한 명의 실패가 다른 봇의 생성을 막지 않는다(봇마다 독립적으로 처리).
// 모든 봇의 게시글 생성이 끝난 뒤에야 상호작용 생성을 시작한다 - 상호작용 후보 우선순위
// (실제 사용자 글 -> 이번 실행 봇 글 -> 기존 봇 글)가 "이번 실행 봇 글"을 반영하려면
// 이번 주 봇들의 게시글이 먼저 전부 저장되어 있어야 하기 때문이다.
@Component
@RequiredArgsConstructor
public class ContentSeedOrchestrator {

    // 시드 마이그레이션이 부여한 닉네임과 1:1로 맞춘 관심사 힌트. MemberProfile에는
    // 이 정보를 저장할 필드가 없어 여기서 정적으로 관리한다 - 5개 봇 닉네임이 생성
    // 카테고리(FOOD/LOVE/BUY/SPORT/WORRY) 5개와 각각 대응하도록 설계되어 있다.
    private static final Map<String, String> INTEREST_HINT_BY_NICKNAME = Map.of(
            "한입만판사", "음식",
            "연애배심원", "연애",
            "장바구니철학자", "소비·쇼핑",
            "숨참고승부", "스포츠·승부",
            "결정은내일", "고민·선택"
    );

    private final BotAccountSelector botAccountSelector;
    private final ClaudeContentGenerator contentGenerator;
    private final ContentQualityGate qualityGate;
    private final ContentSeedCandidateQueryService candidateQueryService;
    private final ContentSeedPersistenceService persistenceService;
    private final ContentSeedProperties properties;

    public ContentSeedRunResult run() {
        List<Member> bots = botAccountSelector.selectActiveBots();
        Set<Long> thisRunBotMemberIds = bots.stream().map(Member::getId).collect(Collectors.toSet());

        List<ContentSeedBatchOutcome> postOutcomes = new ArrayList<>();
        for (Member bot : bots) {
            postOutcomes.add(generatePostsForBot(bot));
        }

        List<ContentSeedBatchOutcome> interactionOutcomes = new ArrayList<>();
        for (Member bot : bots) {
            interactionOutcomes.add(generateInteractionsForBot(bot, thisRunBotMemberIds));
        }

        return new ContentSeedRunResult(postOutcomes, interactionOutcomes);
    }

    private ContentSeedBatchOutcome generatePostsForBot(Member bot) {
        int target = properties.getPostsPerBot();
        List<ContentSeedItemFailure> failures = new ArrayList<>();
        int saved = 0;

        try {
            List<RecentPost> recentPosts = candidateQueryService.findRecentActivePosts();
            List<String> recentTitles = recentPosts.stream().map(RecentPost::title).toList();

            GenerationResult<GeneratedPostBatch> result =
                    contentGenerator.generatePosts(toPersona(bot), target, recentPosts);

            for (GeneratedPost post : result.content().posts()) {
                QualityCheckResult check = qualityGate.validatePost(post, recentTitles);
                String detail = qualityGate.toLogSafeSummary(post.title(), 30);
                if (!check.passed()) {
                    failures.add(new ContentSeedItemFailure(detail, String.join(", ", check.reasons())));
                    continue;
                }
                try {
                    persistenceService.saveBotPost(bot.getId(), post);
                    saved++;
                } catch (Exception e) {
                    failures.add(new ContentSeedItemFailure(detail, describe(e)));
                }
            }
        } catch (Exception e) {
            failures.add(new ContentSeedItemFailure("batch", describe(e)));
        }

        return new ContentSeedBatchOutcome(bot.getId(), target, saved, failures);
    }

    private ContentSeedBatchOutcome generateInteractionsForBot(Member bot, Set<Long> thisRunBotMemberIds) {
        int target = properties.getInteractionsPerBot();
        List<ContentSeedItemFailure> failures = new ArrayList<>();
        int saved = 0;

        try {
            List<CandidatePost> candidates = candidateQueryService.findInteractionCandidates(bot.getId(), thisRunBotMemberIds);
            if (candidates.isEmpty()) {
                return new ContentSeedBatchOutcome(bot.getId(), target, 0, failures);
            }

            Set<Long> candidateIds = candidates.stream().map(CandidatePost::id).collect(Collectors.toSet());
            int requestCount = Math.min(target, candidates.size());

            GenerationResult<GeneratedInteractionBatch> result =
                    contentGenerator.generateInteractions(toPersona(bot), requestCount, candidates);

            Set<Long> usedTargetIds = new HashSet<>();
            for (GeneratedInteraction interaction : result.content().interactions()) {
                String detail = String.valueOf(interaction.targetPostId());

                if (!candidateIds.contains(interaction.targetPostId()) || !usedTargetIds.add(interaction.targetPostId())) {
                    failures.add(new ContentSeedItemFailure(detail, "후보 목록에 없거나 중복 선택된 대상 게시글"));
                    continue;
                }

                QualityCheckResult check = qualityGate.validateInteraction(interaction);
                if (!check.passed()) {
                    failures.add(new ContentSeedItemFailure(detail, String.join(", ", check.reasons())));
                    continue;
                }

                try {
                    persistenceService.saveBotInteraction(bot.getId(), interaction);
                    saved++;
                } catch (Exception e) {
                    failures.add(new ContentSeedItemFailure(detail, describe(e)));
                }
            }
        } catch (Exception e) {
            failures.add(new ContentSeedItemFailure("batch", describe(e)));
        }

        return new ContentSeedBatchOutcome(bot.getId(), target, saved, failures);
    }

    private BotPersonaContext toPersona(Member bot) {
        MemberProfile profile = bot.getProfile();
        if (profile == null) {
            throw new IllegalStateException("봇 회원 " + bot.getId() + "에 프로필이 없습니다.");
        }
        String interestHint = INTEREST_HINT_BY_NICKNAME.getOrDefault(bot.getNickname(), "");
        return new BotPersonaContext(bot.getNickname(), profile.getGender(), profile.getAge(), profile.getMbti(), interestHint);
    }

    private String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
