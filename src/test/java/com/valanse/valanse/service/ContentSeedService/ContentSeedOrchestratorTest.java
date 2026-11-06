package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.MemberProfile;
import com.valanse.valanse.domain.enums.Age;
import com.valanse.valanse.domain.enums.Gender;
import com.valanse.valanse.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PR3-4 "전체 생성 오케스트레이션"의 규칙(모든 봇 게시글 생성 후 상호작용 생성,
 * 봇·배치·항목별 실패 격리, 목표/실제 생성량 모델)이 실제로 지켜지는지 검증하는
 * 단위 테스트입니다. Claude 응답 객체(StructuredMessage 등)는 Kotlin internal
 * 생성자라 목킹이 어려우므로(ClaudeContentGeneratorTest의 기존 결정과 동일),
 * ClaudeContentGenerator 자체를 목으로 대체해 오케스트레이션 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class ContentSeedOrchestratorTest {

    @Mock private BotAccountSelector botAccountSelector;
    @Mock private ClaudeContentGenerator contentGenerator;
    @Mock private ContentSeedCandidateQueryService candidateQueryService;
    @Mock private ContentSeedPersistenceService persistenceService;

    private ContentSeedOrchestrator orchestrator;
    private ContentSeedProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ContentSeedProperties();
        properties.setPostsPerBot(2);
        properties.setInteractionsPerBot(2);
        orchestrator = new ContentSeedOrchestrator(
                botAccountSelector, contentGenerator, new ContentQualityGate(),
                candidateQueryService, persistenceService, properties);
    }

    @Test
    void 모든_봇의_게시글_생성이_끝난_뒤에_상호작용_생성을_시작한다() {
        // postsPerBot 목표를 못 채우는 빈 배치라 부족분 재시도가 발생하므로,
        // generatePosts 호출 횟수 자체가 아니라 "게시글 단계 전체가 상호작용 단계보다
        // 먼저 끝나는지"만 atLeastOnce로 확인한다(정확한 호출 횟수는 다른 테스트가 검증함).
        Member bot1 = bot(1L, "한입만판사");
        Member bot2 = bot(2L, "연애배심원");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot1, bot2));
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        orchestrator.run();

        InOrder order = inOrder(contentGenerator, candidateQueryService);
        order.verify(contentGenerator, org.mockito.Mockito.atLeastOnce()).generatePosts(any(), anyInt(), any());
        order.verify(candidateQueryService).findInteractionCandidates(eq(1L), any()); // bot1 interaction
        order.verify(candidateQueryService).findInteractionCandidates(eq(2L), any()); // bot2 interaction
    }

    @Test
    void 품질_미달_게시글은_저장하지_않고_실패로_기록하며_나머지는_저장한다() {
        // targetCount를 1로 두어 saved(1)==target이 되게 하고, PR3-5의 부족분 재시도가
        // 이 테스트(항목 단위 격리)에 끼어들지 않게 한다 - 재시도는 별도 테스트로 검증함.
        properties.setPostsPerBot(1);
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        GeneratedPost tooLongTitle = new GeneratedPost(
                "가".repeat(30), "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost validPost = new GeneratedPost("정상 제목", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(tooLongTitle, validPost)), 1, 1));
        when(persistenceService.saveBotPost(eq(1L), eq(validPost))).thenReturn(100L);

        ContentSeedRunResult result = orchestrator.run();

        ContentSeedBatchOutcome outcome = result.postOutcomes().get(0);
        assertThat(outcome.savedCount()).isEqualTo(1);
        assertThat(outcome.targetCount()).isEqualTo(1);
        assertThat(outcome.failures()).hasSize(1);
        assertThat(result.savedPostIds()).containsExactly(100L);
        verify(persistenceService, never()).saveBotPost(eq(1L), eq(tooLongTitle));
    }

    @Test
    void 저장_중_예외가_발생해도_다른_항목_저장은_계속된다() {
        properties.setPostsPerBot(1); // 재시도는 별도 테스트로 검증하므로 target==saved로 맞춤
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        GeneratedPost failingPost = new GeneratedPost("실패할 제목", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost okPost = new GeneratedPost("성공할 제목", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(failingPost, okPost)), 1, 1));
        doThrow(new RuntimeException("DB 오류")).when(persistenceService).saveBotPost(eq(1L), eq(failingPost));
        when(persistenceService.saveBotPost(eq(1L), eq(okPost))).thenReturn(200L);

        ContentSeedRunResult result = orchestrator.run();

        ContentSeedBatchOutcome outcome = result.postOutcomes().get(0);
        assertThat(outcome.savedCount()).isEqualTo(1);
        assertThat(outcome.failures()).hasSize(1);
        assertThat(outcome.failures().get(0).reason()).contains("DB 오류");
    }

    @Test
    void 한_봇의_생성_호출이_실패해도_다른_봇_처리는_계속된다() {
        properties.setPostsPerBot(1); // 재시도는 별도 테스트로 검증하므로 target==saved로 맞춤
        Member failingBot = bot(1L, "한입만판사");
        Member okBot = bot(2L, "연애배심원");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(failingBot, okBot));
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        GeneratedPost okPost = new GeneratedPost("정상 제목", "본문", "A", "B", GeneratableVoteCategory.LOVE);
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenThrow(new RuntimeException("Claude 호출 실패"))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(okPost)), 1, 1));
        when(persistenceService.saveBotPost(eq(2L), eq(okPost))).thenReturn(300L);

        ContentSeedRunResult result = orchestrator.run();

        ContentSeedBatchOutcome failingOutcome = result.postOutcomes().get(0);
        assertThat(failingOutcome.savedCount()).isZero();
        assertThat(failingOutcome.failures()).hasSize(1);

        ContentSeedBatchOutcome okOutcome = result.postOutcomes().get(1);
        assertThat(okOutcome.savedCount()).isEqualTo(1);
        assertThat(okOutcome.failures()).isEmpty();
    }

    @Test
    void 상호작용_후보가_없으면_생성_호출_없이_0건으로_처리한다() {
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));
        when(candidateQueryService.findInteractionCandidates(eq(1L), any())).thenReturn(List.of());

        ContentSeedRunResult result = orchestrator.run();

        verify(contentGenerator, never()).generateInteractions(any(), anyInt(), any());
        ContentSeedBatchOutcome outcome = result.interactionOutcomes().get(0);
        assertThat(outcome.savedCount()).isZero();
        assertThat(outcome.targetCount()).isEqualTo(2);
        assertThat(outcome.failures()).isEmpty();
    }

    @Test
    void 후보에_없거나_중복된_대상_상호작용은_거부한다() {
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));

        CandidatePost candidate = new CandidatePost(10L, "제목", "본문", "A", "B", List.of());
        when(candidateQueryService.findInteractionCandidates(eq(1L), any())).thenReturn(List.of(candidate));

        GeneratedInteraction validInteraction =
                new GeneratedInteraction(10L, GeneratedInteraction.SelectedOption.A, "정상 댓글");
        GeneratedInteraction duplicateInteraction =
                new GeneratedInteraction(10L, GeneratedInteraction.SelectedOption.B, "중복 댓글");
        GeneratedInteraction unknownTargetInteraction =
                new GeneratedInteraction(999L, GeneratedInteraction.SelectedOption.A, "알 수 없는 대상");
        when(contentGenerator.generateInteractions(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedInteractionBatch(
                        List.of(validInteraction, duplicateInteraction, unknownTargetInteraction)), 1, 1));
        when(persistenceService.saveBotInteraction(eq(1L), eq(validInteraction))).thenReturn(500L);

        ContentSeedRunResult result = orchestrator.run();

        ContentSeedBatchOutcome outcome = result.interactionOutcomes().get(0);
        assertThat(outcome.savedCount()).isEqualTo(1);
        assertThat(outcome.failures()).hasSize(2);
        verify(persistenceService, never()).saveBotInteraction(eq(1L), eq(duplicateInteraction));
        verify(persistenceService, never()).saveBotInteraction(eq(1L), eq(unknownTargetInteraction));
    }

    @Test
    void 목표에_못_미치면_부족분만_최대_1회_재요청한다() {
        properties.setPostsPerBot(3);
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        GeneratedPost onlyPost = new GeneratedPost("제목", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(onlyPost)), 1, 1))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));
        when(persistenceService.saveBotPost(eq(1L), eq(onlyPost))).thenReturn(100L);

        ContentSeedRunResult result = orchestrator.run();

        ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(contentGenerator, times(2)).generatePosts(any(), countCaptor.capture(), any());
        assertThat(countCaptor.getAllValues()).containsExactly(3, 2); // 1차 목표 3, 재시도는 부족분 2만
        assertThat(result.postOutcomes().get(0).savedCount()).isEqualTo(1); // 재시도도 못 채웠지만 더는 재요청하지 않음
    }

    @Test
    void 사용량_토큰과_예상_비용을_집계한다() {
        properties.setPostsPerBot(1);
        properties.setInteractionsPerBot(1);
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));

        GeneratedPost post = new GeneratedPost("제목", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(post)), 1000, 500));
        when(persistenceService.saveBotPost(eq(1L), eq(post))).thenReturn(100L);

        CandidatePost candidate = new CandidatePost(10L, "제목", "본문", "A", "B", List.of());
        when(candidateQueryService.findInteractionCandidates(eq(1L), any())).thenReturn(List.of(candidate));
        GeneratedInteraction interaction =
                new GeneratedInteraction(10L, GeneratedInteraction.SelectedOption.A, "댓글");
        when(contentGenerator.generateInteractions(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedInteractionBatch(List.of(interaction)), 200, 100));
        when(persistenceService.saveBotInteraction(eq(1L), eq(interaction))).thenReturn(500L);

        ContentSeedUsageSummary usage = orchestrator.run().usage();

        assertThat(usage.apiCallCount()).isEqualTo(2);
        assertThat(usage.inputTokens()).isEqualTo(1200);
        assertThat(usage.outputTokens()).isEqualTo(600);
        // 기본 단가: 입력 $2/M, 출력 $10/M -> (1200*2 + 600*10) / 1,000,000 = 0.0084
        assertThat(usage.estimatedCostUsd()).isEqualByComparingTo(new BigDecimal("0.0084"));
    }

    @Test
    void 댓글_전용_실행은_게시글_생성을_호출하지_않는다() {
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(candidateQueryService.findInteractionCandidates(eq(1L), any(), eq(false))).thenReturn(List.of());

        ContentSeedRunResult result = orchestrator.runCommentsOnly();

        verify(contentGenerator, never()).generatePosts(any(), anyInt(), any());
        assertThat(result.postOutcomes()).isEmpty();
        assertThat(result.targetPostCount()).isZero();
        assertThat(result.savedPostCount()).isZero();
        assertThat(result.savedPostIds()).isEmpty();
    }

    @Test
    void 댓글_전용_실행도_기존_대상_선정_정책으로_투표와_댓글을_저장한다() {
        properties.setInteractionsPerBot(1);
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));

        CandidatePost candidate = new CandidatePost(10L, "제목", "본문", "A", "B", List.of());
        when(candidateQueryService.findInteractionCandidates(eq(1L), any(), eq(false))).thenReturn(List.of(candidate));
        GeneratedInteraction interaction =
                new GeneratedInteraction(10L, GeneratedInteraction.SelectedOption.A, "댓글");
        when(contentGenerator.generateInteractions(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedInteractionBatch(List.of(interaction)), 10, 5));
        when(persistenceService.saveBotInteraction(eq(1L), eq(interaction))).thenReturn(500L);

        ContentSeedRunResult result = orchestrator.runCommentsOnly();

        verify(candidateQueryService).findInteractionCandidates(eq(1L), any(), eq(false));
        verify(persistenceService).saveBotInteraction(eq(1L), eq(interaction));
        ContentSeedBatchOutcome outcome = result.interactionOutcomes().get(0);
        assertThat(outcome.savedCount()).isEqualTo(1);
        assertThat(outcome.targetCount()).isEqualTo(1);
    }

    @Test
    void 댓글_전용_실행은_targetVoteLookbackDays_제한_없이_전체_게시글을_후보로_조회한다() {
        // 이슈 #328: 관리자 댓글 전용 수동 실행은 최근 7일 제한을 적용하는 2-인자
        // 메서드가 아니라, 제한을 끄는 3-인자(applyRecencyLimit=false) 메서드를 호출해야 한다.
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(candidateQueryService.findInteractionCandidates(eq(1L), any(), eq(false))).thenReturn(List.of());

        orchestrator.runCommentsOnly();

        verify(candidateQueryService, never()).findInteractionCandidates(any(), any());
        verify(candidateQueryService).findInteractionCandidates(eq(1L), any(), eq(false));
    }

    @Test
    void 전체_생성_실행은_여전히_최근_7일_제한_정책으로_후보를_조회한다() {
        // 이슈 #328: 주간 자동 콘텐츠 생성(run())은 기존 2-인자(제한 적용) 메서드를
        // 그대로 호출해야 한다 - 제한을 끄는 3-인자 메서드를 호출하면 안 된다.
        Member bot = bot(1L, "한입만판사");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));
        when(candidateQueryService.findInteractionCandidates(eq(1L), any())).thenReturn(List.of());

        orchestrator.run();

        verify(candidateQueryService).findInteractionCandidates(eq(1L), any());
        verify(candidateQueryService, never()).findInteractionCandidates(any(), any(), anyBoolean());
    }

    @Test
    void 닉네임이_바뀌어도_관심사_매핑은_봇_식별자_기준으로_유지된다() {
        // REQ-001/AC-004: 관심사·말투 힌트는 닉네임이 아니라 안정적인 social_id로
        // 조회되어야 한다. 과거 매핑 키였던 닉네임("한입만판사")이 아닌 다른
        // 닉네임을 쓰더라도 social_id가 같으면 동일한 관심사 힌트를 받아야 한다.
        properties.setPostsPerBot(1);
        Member bot = Member.builder()
                .id(1L)
                .socialId("content-seed-bot-001")
                .nickname("새로운닉네임")
                .isBot(true)
                .role(Role.USER)
                .profile(MemberProfile.builder().gender(Gender.FEMALE).age(Age.TWENTY).mbti("ENFP").build())
                .build();
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot));
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));

        orchestrator.run();

        ArgumentCaptor<BotPersonaContext> personaCaptor = ArgumentCaptor.forClass(BotPersonaContext.class);
        verify(contentGenerator, org.mockito.Mockito.atLeastOnce())
                .generatePosts(personaCaptor.capture(), anyInt(), any());
        BotPersonaContext capturedPersona = personaCaptor.getValue();
        assertThat(capturedPersona.nickname()).isEqualTo("새로운닉네임");
        assertThat(capturedPersona.interestHint())
                .isEqualTo(BotPersonaCatalog.traitsFor("content-seed-bot-001").interestHint());
        assertThat(capturedPersona.interestHint()).isNotBlank();
    }

    // M3/REQ-006/AC-013: "같은 봇 게시글 연속" 패턴이 생기지 않도록, 여러 봇의
    // 게시글 생성 항목이 저장되는 순서가 봇 간에 교차되어야 한다 - 동일 봇의 저장
    // 호출이 연속 2개를 넘지 않아야 한다.
    @Test
    void 여러_봇_게시글_생성에서_동일_봇_저장이_연속_2개를_넘지_않는다() {
        properties.setPostsPerBot(2);
        Member bot1 = bot(1L, "닉네임1");
        Member bot2 = bot(2L, "닉네임2");
        Member bot3 = bot(3L, "닉네임3");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot1, bot2, bot3));
        when(candidateQueryService.findRecentActivePosts()).thenReturn(List.of());
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        GeneratedPost itemA = new GeneratedPost("제목A", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost itemB = new GeneratedPost("제목B", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        when(contentGenerator.generatePosts(any(), eq(2), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(itemA, itemB)), 1, 1));
        when(persistenceService.saveBotPost(any(), any())).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);

        orchestrator.run();

        ArgumentCaptor<Long> botIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(persistenceService, times(6)).saveBotPost(botIdCaptor.capture(), any());
        assertMaxConsecutiveRun(botIdCaptor.getAllValues(), 2);
    }

    // M3/REQ-006/AC-013/AC-016: run()의 상호작용 단계에서도 동일하게 동일 봇 저장이
    // 연속 2개를 넘지 않아야 한다(봇과 후보 게시글 조합을 교차 배정).
    @Test
    void 여러_봇_상호작용_생성에서도_동일_봇_저장이_연속_2개를_넘지_않는다() {
        properties.setPostsPerBot(0);
        properties.setInteractionsPerBot(2);
        Member bot1 = bot(1L, "닉네임1");
        Member bot2 = bot(2L, "닉네임2");
        Member bot3 = bot(3L, "닉네임3");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot1, bot2, bot3));
        when(candidateQueryService.findRecentActivePosts()).thenReturn(List.of());
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));

        CandidatePost candidateA = new CandidatePost(10L, "제목A", "본문", "A", "B", List.of());
        CandidatePost candidateB = new CandidatePost(11L, "제목B", "본문", "A", "B", List.of());
        when(candidateQueryService.findInteractionCandidates(any(), any()))
                .thenReturn(List.of(candidateA, candidateB));

        GeneratedInteraction interactionA =
                new GeneratedInteraction(10L, GeneratedInteraction.SelectedOption.A, "댓글A");
        GeneratedInteraction interactionB =
                new GeneratedInteraction(11L, GeneratedInteraction.SelectedOption.B, "댓글B");
        when(contentGenerator.generateInteractions(any(), eq(2), any()))
                .thenReturn(new GenerationResult<>(new GeneratedInteractionBatch(List.of(interactionA, interactionB)), 1, 1));

        orchestrator.run();

        ArgumentCaptor<Long> botIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(persistenceService, times(6)).saveBotInteraction(botIdCaptor.capture(), any());
        assertMaxConsecutiveRun(botIdCaptor.getAllValues(), 2);
    }

    // M3/REQ-006/AC-016: runCommentsOnly()에도 run()과 동일한 분산 규칙이 적용되어야
    // 한다(게시글 생성 없이 상호작용만).
    @Test
    void 댓글_전용_실행에서도_동일_봇_저장이_연속_2개를_넘지_않는다() {
        properties.setInteractionsPerBot(2);
        Member bot1 = bot(1L, "닉네임1");
        Member bot2 = bot(2L, "닉네임2");
        Member bot3 = bot(3L, "닉네임3");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot1, bot2, bot3));

        CandidatePost candidateA = new CandidatePost(10L, "제목A", "본문", "A", "B", List.of());
        CandidatePost candidateB = new CandidatePost(11L, "제목B", "본문", "A", "B", List.of());
        when(candidateQueryService.findInteractionCandidates(any(), any(), eq(false)))
                .thenReturn(List.of(candidateA, candidateB));

        GeneratedInteraction interactionA =
                new GeneratedInteraction(10L, GeneratedInteraction.SelectedOption.A, "댓글A");
        GeneratedInteraction interactionB =
                new GeneratedInteraction(11L, GeneratedInteraction.SelectedOption.B, "댓글B");
        when(contentGenerator.generateInteractions(any(), eq(2), any()))
                .thenReturn(new GenerationResult<>(new GeneratedInteractionBatch(List.of(interactionA, interactionB)), 1, 1));

        orchestrator.runCommentsOnly();

        verify(contentGenerator, never()).generatePosts(any(), anyInt(), any());
        ArgumentCaptor<Long> botIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(persistenceService, times(6)).saveBotInteraction(botIdCaptor.capture(), any());
        assertMaxConsecutiveRun(botIdCaptor.getAllValues(), 2);
    }

    // 코드리뷰 지적: 봇별 유효 항목 수가 크게 다르면(예: 4건 vs 1건) 기존
    // interleaveRoundRobin(라운드마다 모든 그룹의 round번째 인덱스를 순서대로 추가)이
    // 짧은 그룹이 먼저 바닥나는 즉시 남은 긴 그룹의 나머지 전부를 꼬리에 몰아
    // 연속 2개를 넘기는 사례(REQ-006/AC-013 위반)를 재현한다. [4,1]은 수학적으로
    // 달성 가능한(feasible) 분포다 - maxCount=4, total=5, rest=1이고
    // 4 <= 2*(5-4+1)=4 를 만족하므로, 올바른 알고리즘이면 연속 2개를 넘지 않는
    // 배치(예: A,A,B,A,A)를 만들 수 있어야 한다.
    @Test
    void 봇별_유효_항목_수가_크게_달라도_feasible한_경우_동일_봇_저장이_연속_2개를_넘지_않는다() {
        properties.setPostsPerBot(1); // target을 실제 달성량보다 낮게 둬 부족분 재시도가 끼어들지 않게 함
        Member bot1 = bot(1L, "닉네임1");
        Member bot2 = bot(2L, "닉네임2");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot1, bot2));
        when(candidateQueryService.findRecentActivePosts()).thenReturn(List.of());
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        GeneratedPost a0 = new GeneratedPost("제목A0", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost a1 = new GeneratedPost("제목A1", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost a2 = new GeneratedPost("제목A2", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost a3 = new GeneratedPost("제목A3", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost b0 = new GeneratedPost("제목B0", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(a0, a1, a2, a3)), 1, 1))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(b0)), 1, 1));
        when(persistenceService.saveBotPost(any(), any())).thenReturn(1L, 2L, 3L, 4L, 5L);

        orchestrator.run();

        ArgumentCaptor<Long> botIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(persistenceService, times(5)).saveBotPost(botIdCaptor.capture(), any());
        assertMaxConsecutiveRun(botIdCaptor.getAllValues(), 2);
    }

    // [5,1](maxCount=5, total=6, rest=1)은 5 <= 2*(6-5+1)=4가 거짓이므로 연속 2개
    // 이하로는 어떤 알고리즘도 배치할 수 없는 수학적으로 불가능한(infeasible) 분포다.
    // 이 경우 달성 가능한 최선은 "최소 가능한 최대 연속 길이" 공식
    // ceil(maxCount / (total - maxCount + 1)) = ceil(5/2) = 3 이다 - 즉 올바른
    // 탐욕 알고리즘이라면 연속 3개(그 이상은 아님)까지만 허용해야 한다. 기존
    // interleaveRoundRobin은 이 사례에서 연속 4개(A,A,A,A)를 만들어 그 최적값보다도
    // 나쁘다 - 이 테스트는 "feasible 영역이 아니라도 가능한 한 최선으로 분산"하는지
    // 검증한다.
    @Test
    void 봇별_유효_항목_수_편차가_극단적인_infeasible_분포에서도_수학적_최선의_분산을_달성한다() {
        properties.setPostsPerBot(1);
        Member bot1 = bot(1L, "닉네임1");
        Member bot2 = bot(2L, "닉네임2");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot1, bot2));
        when(candidateQueryService.findRecentActivePosts()).thenReturn(List.of());
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        GeneratedPost a0 = new GeneratedPost("제목A0", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost a1 = new GeneratedPost("제목A1", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost a2 = new GeneratedPost("제목A2", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost a3 = new GeneratedPost("제목A3", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost a4 = new GeneratedPost("제목A4", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        GeneratedPost b0 = new GeneratedPost("제목B0", "본문", "A", "B", GeneratableVoteCategory.FOOD);
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(a0, a1, a2, a3, a4)), 1, 1))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of(b0)), 1, 1));
        when(persistenceService.saveBotPost(any(), any())).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);

        orchestrator.run();

        ArgumentCaptor<Long> botIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(persistenceService, times(6)).saveBotPost(botIdCaptor.capture(), any());
        // feasible 상한(2)이 아니라 infeasible 분포의 수학적 최적값(3)으로 검증한다.
        assertMaxConsecutiveRun(botIdCaptor.getAllValues(), 3);
    }

    private void assertMaxConsecutiveRun(List<Long> saveOrder, int maxAllowed) {
        int run = 1;
        for (int i = 1; i < saveOrder.size(); i++) {
            if (saveOrder.get(i).equals(saveOrder.get(i - 1))) {
                run++;
            } else {
                run = 1;
            }
            assertThat(run)
                    .as("저장 순서 %s 중 인덱스 %d에서 동일 봇(%d) 연속 횟수", saveOrder, i, saveOrder.get(i))
                    .isLessThanOrEqualTo(maxAllowed);
        }
    }

    @Test
    void 실패_사유별_개수를_집계한다() {
        ContentSeedBatchOutcome postOutcome = new ContentSeedBatchOutcome(1L, 2, 1, List.of(
                new ContentSeedItemFailure("제목1", "제목 길이 위반(1~25자)")));
        ContentSeedBatchOutcome interactionOutcome = new ContentSeedBatchOutcome(1L, 2, 1, List.of(
                new ContentSeedItemFailure("10", "제목 길이 위반(1~25자)"),
                new ContentSeedItemFailure("11", "선택 옵션 누락")));
        ContentSeedRunResult result = new ContentSeedRunResult(
                List.of(postOutcome), List.of(interactionOutcome), ContentSeedUsageSummary.empty(), List.of());

        Map<String, Long> counts = result.rejectionReasonCounts();

        assertThat(counts).containsEntry("제목 길이 위반(1~25자)", 2L);
        assertThat(counts).containsEntry("선택 옵션 누락", 1L);
    }

    private Member bot(long id, String nickname) {
        MemberProfile profile = MemberProfile.builder()
                .gender(Gender.FEMALE)
                .age(Age.TWENTY)
                .mbti("ENFP")
                .build();
        return Member.builder()
                .id(id)
                .nickname(nickname)
                .isBot(true)
                .role(Role.USER)
                .profile(profile)
                .build();
    }
}
