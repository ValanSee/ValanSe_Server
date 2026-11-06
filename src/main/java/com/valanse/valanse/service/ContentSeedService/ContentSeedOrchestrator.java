package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
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
// 목표 수량에 못 미치면(품질 거절·저장 실패 등) 부족분만 최대 1회 재요청한다.
//
// M3(REQ-006, plan.md Milestone M3): 생성(+품질검증)과 저장(persist)을 두 단계로
// 나눈다 - 생성 단계는 기존처럼 봇별로 순차 처리하지만(재요청/실패 격리 로직은
// 그대로), 실제 저장(saveBotPost/saveBotInteraction) 호출만 봇 간에 탐욕(greedy)
// 알고리즘으로 교차시킨다(interleaveRoundRobin). 남은 항목이 가장 많은 봇을
// 우선 배치하고, 같은 봇이 연속 2개가 되면 다른 봇으로 강제 전환하므로, 그
// 전환이 수학적으로 가능한 분포(그룹 크기 [maxCount, rest...]에서
// maxCount <= 2*(total-maxCount+1))에서는 같은 봇의 작업이 연속 2개를 넘어
// 이어지지 않는다(AC-013). 그 부등식이 거짓인 극단적 편차에서는 가능한 최선
// (연속 ceil(maxCount/(total-maxCount+1))개)까지만 보장한다 - 상세는
// interleaveRoundRobin의 @MX:DEBT 참고. 목표량/부족분 재시도/실패 격리는
// 생성 단계 로직을 그대로 유지하므로 기존과 동일하게 동작한다(AC-014/AC-015).
@Component
@RequiredArgsConstructor
public class ContentSeedOrchestrator {

    private final BotAccountSelector botAccountSelector;
    private final ClaudeContentGenerator contentGenerator;
    private final ContentQualityGate qualityGate;
    private final ContentSeedCandidateQueryService candidateQueryService;
    private final ContentSeedPersistenceService persistenceService;
    private final ContentSeedProperties properties;

    public ContentSeedRunResult run() {
        List<Member> bots = botAccountSelector.selectActiveBots();
        Set<Long> thisRunBotMemberIds = bots.stream().map(Member::getId).collect(Collectors.toSet());
        UsageAccumulator usage = new UsageAccumulator();

        PostsPhaseResult postsResult = generatePostsForAllBots(bots, usage);
        List<ContentSeedBatchOutcome> interactionOutcomes =
                generateInteractionsForAllBots(bots, thisRunBotMemberIds, usage, true);

        return new ContentSeedRunResult(
                postsResult.outcomes(), interactionOutcomes,
                usage.toSummary(properties.getPricing()), List.copyOf(postsResult.savedPostIds()));
    }

    // 댓글(투표+댓글) 전용 실행 경로. run()과 봇 선정·대상 선정 정책은 그대로 재사용하되,
    // 게시글 생성 단계(generatePostsForAllBots)는 호출하지 않는다. 이번 실행에서 새로 저장되는
    // 게시글이 없으므로 "이번 실행 봇 글" 우선순위 태그 대상도 없다 - 빈 집합을 넘겨 모든
    // 기존 봇 글이 "기존 봇 글" 우선순위로 정확히 분류되게 한다(priorityTier 참고).
    // 이슈 #328: 관리자 댓글 전용 수동 실행은 targetVoteLookbackDays 제한 없이 전체
    // 게시글을 후보로 조회한다(applyRecencyLimit=false) - 주간 자동 생성(run())만 7일
    // 제한을 유지한다. M3: 분산 규칙(generateInteractionsForAllBots)은 run()과 동일하게 적용된다.
    public ContentSeedRunResult runCommentsOnly() {
        List<Member> bots = botAccountSelector.selectActiveBots();
        UsageAccumulator usage = new UsageAccumulator();

        List<ContentSeedBatchOutcome> interactionOutcomes =
                generateInteractionsForAllBots(bots, Set.of(), usage, false);

        return new ContentSeedRunResult(
                List.of(), interactionOutcomes, usage.toSummary(properties.getPricing()), List.of());
    }

    // 모든 봇의 게시글을 생성한다 - 1차 시도를 전체 봇에 대해 순차 생성(+품질검증)한 뒤
    // 라운드로빈으로 교차 저장하고, 그다음 부족분이 남은 봇들만 재시도(생성+교차저장)한다.
    private PostsPhaseResult generatePostsForAllBots(List<Member> bots, UsageAccumulator usage) {
        int target = properties.getPostsPerBot();
        Map<Long, List<ContentSeedItemFailure>> failuresByBot = initFailureMap(bots);
        Map<Long, Integer> savedCountByBot = initZeroMap(bots);
        List<Long> savedPostIds = new ArrayList<>();

        Map<Long, List<RecentPost>> recentPostsByBot = new HashMap<>();
        Map<Long, List<String>> recentTitlesByBot = new HashMap<>();

        // 1차 시도에서 "batch" 예외(연결 실패 등)가 난 봇은 재시도를 하지 않는다 -
        // 기존 generatePostsForBot()이 1차+재시도 전체를 하나의 try로 묶어, 1차에서
        // 예외가 나면 재시도 없이 그 봇의 처리를 즉시 중단하던 동작과 동일하게 맞춘다.
        Set<Long> batchErrorBots = new HashSet<>();

        List<List<PostDraft>> firstAttempt = new ArrayList<>();
        for (Member bot : bots) {
            List<PostDraft> drafts = List.of();
            try {
                List<RecentPost> recentPosts = candidateQueryService.findRecentActivePosts();
                List<String> recentTitles = recentPosts.stream().map(RecentPost::title).toList();
                recentPostsByBot.put(bot.getId(), recentPosts);
                recentTitlesByBot.put(bot.getId(), recentTitles);
                drafts = collectPostDrafts(bot, target, recentPosts, recentTitles, usage, failuresByBot);
            } catch (Exception e) {
                failuresByBot.get(bot.getId()).add(new ContentSeedItemFailure("batch", describe(e)));
                batchErrorBots.add(bot.getId());
            }
            firstAttempt.add(drafts);
        }
        persistPostDraftsInterleaved(firstAttempt, savedCountByBot, savedPostIds, failuresByBot);

        List<List<PostDraft>> retry = new ArrayList<>();
        for (Member bot : bots) {
            List<PostDraft> drafts = List.of();
            List<RecentPost> recentPosts = recentPostsByBot.get(bot.getId());
            int shortfall = target - savedCountByBot.get(bot.getId());
            if (!batchErrorBots.contains(bot.getId()) && recentPosts != null && shortfall > 0) {
                try {
                    drafts = collectPostDrafts(
                            bot, shortfall, recentPosts, recentTitlesByBot.get(bot.getId()), usage, failuresByBot);
                } catch (Exception e) {
                    failuresByBot.get(bot.getId()).add(new ContentSeedItemFailure("batch", describe(e)));
                }
            }
            retry.add(drafts);
        }
        persistPostDraftsInterleaved(retry, savedCountByBot, savedPostIds, failuresByBot);

        List<ContentSeedBatchOutcome> outcomes = bots.stream()
                .map(bot -> new ContentSeedBatchOutcome(
                        bot.getId(), target, savedCountByBot.get(bot.getId()), failuresByBot.get(bot.getId())))
                .toList();
        return new PostsPhaseResult(outcomes, savedPostIds);
    }

    // 한 봇의 게시글 생성 1회 시도(최초 또는 재시도)를 수행하고, 품질 검증을 통과한
    // 항목만 "저장 대기" 상태로 모아 반환한다 - 실제 저장은 호출자가 라운드로빈으로
    // 교차시킨 뒤 수행한다. 생성 호출 자체의 예외는 호출자가 처리한다(전파).
    private List<PostDraft> collectPostDrafts(
            Member bot, int count, List<RecentPost> recentPosts, List<String> recentTitles,
            UsageAccumulator usage, Map<Long, List<ContentSeedItemFailure>> failuresByBot) {
        GenerationResult<GeneratedPostBatch> result = contentGenerator.generatePosts(toPersona(bot), count, recentPosts);
        usage.record(result.inputTokens(), result.outputTokens());

        List<PostDraft> drafts = new ArrayList<>();
        for (GeneratedPost post : result.content().posts()) {
            QualityCheckResult check = qualityGate.validatePost(post, recentTitles);
            String detail = qualityGate.toLogSafeSummary(post.title(), 30);
            if (!check.passed()) {
                failuresByBot.get(bot.getId()).add(new ContentSeedItemFailure(detail, String.join(", ", check.reasons())));
                continue;
            }
            drafts.add(new PostDraft(bot.getId(), post, detail));
        }
        return drafts;
    }

    private void persistPostDraftsInterleaved(
            List<List<PostDraft>> perBotDrafts, Map<Long, Integer> savedCountByBot,
            List<Long> savedPostIds, Map<Long, List<ContentSeedItemFailure>> failuresByBot) {
        for (PostDraft draft : interleaveRoundRobin(perBotDrafts)) {
            try {
                savedPostIds.add(persistenceService.saveBotPost(draft.botId(), draft.post()));
                savedCountByBot.merge(draft.botId(), 1, Integer::sum);
            } catch (Exception e) {
                failuresByBot.get(draft.botId()).add(new ContentSeedItemFailure(draft.detail(), describe(e)));
            }
        }
    }

    // 모든 봇의 상호작용(투표+댓글)을 생성한다. run()/runCommentsOnly() 모두 이 메서드를
    // 공유하며, applyRecencyLimit으로 최근 7일 제한 여부(이슈 #328)만 다르게 적용한다.
    private List<ContentSeedBatchOutcome> generateInteractionsForAllBots(
            List<Member> bots, Set<Long> thisRunBotMemberIds, UsageAccumulator usage, boolean applyRecencyLimit) {
        int target = properties.getInteractionsPerBot();
        Map<Long, List<ContentSeedItemFailure>> failuresByBot = initFailureMap(bots);
        Map<Long, Integer> savedCountByBot = initZeroMap(bots);

        Map<Long, List<CandidatePost>> candidatesByBot = new HashMap<>();
        Map<Long, Set<Long>> candidateIdsByBot = new HashMap<>();
        Map<Long, Set<Long>> usedTargetIdsByBot = new HashMap<>();

        // 1차 시도에서 "batch" 예외가 난 봇은 재시도를 하지 않는다 - 기존
        // generateInteractionsForBot()이 1차+재시도 전체를 하나의 try로 묶었던 동작과 동일.
        Set<Long> batchErrorBots = new HashSet<>();

        List<List<InteractionDraft>> firstAttempt = new ArrayList<>();
        for (Member bot : bots) {
            List<InteractionDraft> drafts = List.of();
            try {
                List<CandidatePost> candidates = applyRecencyLimit
                        ? candidateQueryService.findInteractionCandidates(bot.getId(), thisRunBotMemberIds)
                        : candidateQueryService.findInteractionCandidates(bot.getId(), thisRunBotMemberIds, false);
                candidatesByBot.put(bot.getId(), candidates);
                if (!candidates.isEmpty()) {
                    Set<Long> candidateIds = candidates.stream().map(CandidatePost::id).collect(Collectors.toSet());
                    Set<Long> usedTargetIds = new HashSet<>();
                    candidateIdsByBot.put(bot.getId(), candidateIds);
                    usedTargetIdsByBot.put(bot.getId(), usedTargetIds);

                    int firstCount = Math.min(target, candidates.size());
                    drafts = collectInteractionDrafts(
                            bot, firstCount, candidates, candidateIds, usedTargetIds, usage, failuresByBot);
                }
            } catch (Exception e) {
                failuresByBot.get(bot.getId()).add(new ContentSeedItemFailure("batch", describe(e)));
                batchErrorBots.add(bot.getId());
            }
            firstAttempt.add(drafts);
        }
        persistInteractionDraftsInterleaved(firstAttempt, savedCountByBot, failuresByBot);

        List<List<InteractionDraft>> retry = new ArrayList<>();
        for (Member bot : bots) {
            List<InteractionDraft> drafts = List.of();
            List<CandidatePost> candidates = candidatesByBot.get(bot.getId());
            if (!batchErrorBots.contains(bot.getId()) && candidates != null && !candidates.isEmpty()) {
                Set<Long> candidateIds = candidateIdsByBot.get(bot.getId());
                Set<Long> usedTargetIds = usedTargetIdsByBot.get(bot.getId());
                int shortfall = target - savedCountByBot.get(bot.getId());
                int remainingCandidates = candidates.size() - usedTargetIds.size();
                if (shortfall > 0 && remainingCandidates > 0) {
                    try {
                        drafts = collectInteractionDrafts(
                                bot, Math.min(shortfall, remainingCandidates), candidates, candidateIds, usedTargetIds,
                                usage, failuresByBot);
                    } catch (Exception e) {
                        failuresByBot.get(bot.getId()).add(new ContentSeedItemFailure("batch", describe(e)));
                    }
                }
            }
            retry.add(drafts);
        }
        persistInteractionDraftsInterleaved(retry, savedCountByBot, failuresByBot);

        return bots.stream()
                .map(bot -> new ContentSeedBatchOutcome(
                        bot.getId(), target, savedCountByBot.get(bot.getId()), failuresByBot.get(bot.getId())))
                .toList();
    }

    // 한 봇의 상호작용 생성 1회 시도(최초 또는 재시도)를 수행하고, 후보 검증 및 품질
    // 검증을 통과한 항목만 "저장 대기" 상태로 모아 반환한다. candidateIds/usedTargetIds는
    // 봇별 상태이며 호출자가 1차/재시도에 걸쳐 같은 참조를 유지해 전달한다.
    private List<InteractionDraft> collectInteractionDrafts(
            Member bot, int count, List<CandidatePost> candidates, Set<Long> candidateIds, Set<Long> usedTargetIds,
            UsageAccumulator usage, Map<Long, List<ContentSeedItemFailure>> failuresByBot) {
        GenerationResult<GeneratedInteractionBatch> result =
                contentGenerator.generateInteractions(toPersona(bot), count, candidates);
        usage.record(result.inputTokens(), result.outputTokens());

        List<InteractionDraft> drafts = new ArrayList<>();
        for (GeneratedInteraction interaction : result.content().interactions()) {
            String detail = String.valueOf(interaction.targetPostId());

            if (!candidateIds.contains(interaction.targetPostId()) || !usedTargetIds.add(interaction.targetPostId())) {
                failuresByBot.get(bot.getId()).add(new ContentSeedItemFailure(detail, "후보 목록에 없거나 중복 선택된 대상 게시글"));
                continue;
            }

            QualityCheckResult check = qualityGate.validateInteraction(interaction);
            if (!check.passed()) {
                failuresByBot.get(bot.getId()).add(new ContentSeedItemFailure(detail, String.join(", ", check.reasons())));
                continue;
            }

            drafts.add(new InteractionDraft(bot.getId(), interaction, detail));
        }
        return drafts;
    }

    private void persistInteractionDraftsInterleaved(
            List<List<InteractionDraft>> perBotDrafts, Map<Long, Integer> savedCountByBot,
            Map<Long, List<ContentSeedItemFailure>> failuresByBot) {
        for (InteractionDraft draft : interleaveRoundRobin(perBotDrafts)) {
            try {
                persistenceService.saveBotInteraction(draft.botId(), draft.interaction());
                savedCountByBot.merge(draft.botId(), 1, Integer::sum);
            } catch (Exception e) {
                failuresByBot.get(draft.botId()).add(new ContentSeedItemFailure(draft.detail(), describe(e)));
            }
        }
    }

    // 봇별 목록들을 "남은 항목이 가장 많은 그룹을 우선 배치하되, 같은 그룹이
    // 연속 2개를 넘기려는 순간에는 다른 비어있지 않은 그룹으로 강제 전환"하는
    // 탐욕(greedy) 알고리즘으로 교차시킨다(Task Scheduler/Rearrange String k
    // Distance Apart류와 동일한 계열). 각 그룹 내부의 순서는 그대로 유지된다.
    //
    // 봇별 유효 항목 수가 비슷할 때만 연속을 줄여주던 기존 "라운드마다 모든 그룹의
    // round번째 인덱스를 추가" 방식과 달리, 그룹 크기가 크게 달라도(예: [4,1]) 동일
    // 그룹 연속 2개 초과를 피할 수 있는 한 반드시 피한다.
    //
    // @MX:DEBT: 그룹 크기 분포 [maxCount, rest...]에서 연속 길이를 2 이하로 유지하는
    // 배치가 가능한 것은 maxCount <= 2*(total-maxCount+1)일 때뿐이다(스케줄링
    // "cooldown" 하한). 이 부등식이 거짓인 극단적 편차(예: [5,1], 5 <= 2*(6-5+1)=4가
    // 거짓)에서는 어떤 알고리즘도 REQ-006(연속 2개 이하)을 완전히 만족시킬 수 없다 -
    // 이 메서드는 그 경우 수학적으로 가능한 최선(ceil(maxCount/(total-maxCount+1))
    // 연속)까지만 보장한다.
    // @MX:CEILING: postsPerBot/interactionsPerBot은 모든 봇에 동일하게 적용되는
    // 목표량이지만(ContentSeedProperties), 품질 검증 거부·후보 소진으로 인해 한
    // 실행(firstAttempt 또는 retry 단계 각각)에서 실제로 저장 대기 중인 항목 수는
    // 봇마다 0~target까지 벌어질 수 있다. 특히 botsPerRun=2로 한 봇의 배치가 전부
    // 거부되는 극단적 상황에서는 위 부등식이 거짓이 되어 infeasible 영역에 들어갈
    // 수 있다 - 즉 이 한계는 이론적 가능성이며 무시해도 되는 경계가 아니다.
    // @MX:UPGRADE: postsPerBot/interactionsPerBot이 봇별로 다른 목표 비율(예: 2:1
    // 초과)을 설정할 수 있도록 바뀌면, infeasible 영역에 들어갈 가능성이 구조적으로
    // 높아지므로 이 메서드의 보장 범위를 다시 검토해야 한다.
    private static <T> List<T> interleaveRoundRobin(List<List<T>> perGroupItems) {
        int groupCount = perGroupItems.size();
        int[] cursor = new int[groupCount];
        int[] remaining = new int[groupCount];
        int total = 0;
        for (int i = 0; i < groupCount; i++) {
            remaining[i] = perGroupItems.get(i).size();
            total += remaining[i];
        }

        List<T> merged = new ArrayList<>(total);
        int lastGroup = -1;
        int lastRunLength = 0;
        for (int placed = 0; placed < total; placed++) {
            int chosen = pickNextGroup(remaining, lastGroup, lastRunLength);
            merged.add(perGroupItems.get(chosen).get(cursor[chosen]++));
            remaining[chosen]--;
            lastRunLength = (chosen == lastGroup) ? lastRunLength + 1 : 1;
            lastGroup = chosen;
        }
        return merged;
    }

    // 다음에 배치할 그룹을 고른다: 직전 그룹이 이미 연속 2개째이면(lastRunLength>=2)
    // 그 그룹을 연속 3개째로 만들지 않도록 제외하고, 남은 그룹 중 remaining이 가장
    // 큰 그룹을 고른다. 제외 후 고를 수 있는 다른 그룹이 전혀 없으면(= 수학적으로
    // 불가능한 영역) 어쩔 수 없이 직전 그룹을 다시 선택한다.
    private static int pickNextGroup(int[] remaining, int lastGroup, int lastRunLength) {
        boolean avoidLastGroup = lastRunLength >= 2;
        int best = -1;
        for (int i = 0; i < remaining.length; i++) {
            if (remaining[i] <= 0) {
                continue;
            }
            if (avoidLastGroup && i == lastGroup) {
                continue;
            }
            if (best == -1 || remaining[i] > remaining[best]) {
                best = i;
            }
        }
        return best != -1 ? best : lastGroup;
    }

    private static Map<Long, List<ContentSeedItemFailure>> initFailureMap(List<Member> bots) {
        Map<Long, List<ContentSeedItemFailure>> map = new HashMap<>();
        for (Member bot : bots) {
            map.put(bot.getId(), new ArrayList<>());
        }
        return map;
    }

    private static Map<Long, Integer> initZeroMap(List<Member> bots) {
        Map<Long, Integer> map = new HashMap<>();
        for (Member bot : bots) {
            map.put(bot.getId(), 0);
        }
        return map;
    }

    private BotPersonaContext toPersona(Member bot) {
        MemberProfile profile = bot.getProfile();
        if (profile == null) {
            throw new IllegalStateException("봇 회원 " + bot.getId() + "에 프로필이 없습니다.");
        }
        BotPersonaCatalog.BotPersonaTraits traits = BotPersonaCatalog.traitsFor(bot.getSocialId());
        return new BotPersonaContext(
                bot.getNickname(), profile.getGender(), profile.getAge(), profile.getMbti(),
                traits.interestHint(), traits.toneExample());
    }

    private String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    // 품질 검증을 통과해 저장을 기다리는 게시글 1건. detail은 저장(persist) 실패 시
    // 실패 사유에 함께 기록할 요약 문자열이다(기존 attemptPosts의 detail과 동일 용도).
    private record PostDraft(Long botId, GeneratedPost post, String detail) {
    }

    // 후보/품질 검증을 통과해 저장을 기다리는 상호작용 1건.
    private record InteractionDraft(Long botId, GeneratedInteraction interaction, String detail) {
    }

    private record PostsPhaseResult(List<ContentSeedBatchOutcome> outcomes, List<Long> savedPostIds) {
    }

    // 실행 1회 동안의 API 호출 수·토큰 사용량을 누적하는 내부 집계기.
    private static final class UsageAccumulator {
        private int apiCallCount = 0;
        private long inputTokens = 0;
        private long outputTokens = 0;

        void record(long input, long output) {
            apiCallCount++;
            inputTokens += input;
            outputTokens += output;
        }

        ContentSeedUsageSummary toSummary(ContentSeedProperties.Pricing pricing) {
            BigDecimal estimatedCost = estimateCost(inputTokens, pricing.getInputTokenPricePerMillionUsd())
                    .add(estimateCost(outputTokens, pricing.getOutputTokenPricePerMillionUsd()));
            return new ContentSeedUsageSummary(apiCallCount, inputTokens, outputTokens, estimatedCost);
        }

        private BigDecimal estimateCost(long tokens, BigDecimal pricePerMillion) {
            return pricePerMillion
                    .multiply(BigDecimal.valueOf(tokens))
                    .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        }
    }
}
