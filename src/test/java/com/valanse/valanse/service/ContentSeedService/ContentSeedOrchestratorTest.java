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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
        Member bot1 = bot(1L, "한입만판사");
        Member bot2 = bot(2L, "연애배심원");
        when(botAccountSelector.selectActiveBots()).thenReturn(List.of(bot1, bot2));
        when(contentGenerator.generatePosts(any(), anyInt(), any()))
                .thenReturn(new GenerationResult<>(new GeneratedPostBatch(List.of()), 1, 1));
        when(candidateQueryService.findInteractionCandidates(any(), any())).thenReturn(List.of());

        orchestrator.run();

        InOrder order = inOrder(contentGenerator, candidateQueryService);
        order.verify(contentGenerator).generatePosts(any(), anyInt(), any()); // bot1 post
        order.verify(contentGenerator).generatePosts(any(), anyInt(), any()); // bot2 post
        order.verify(candidateQueryService).findInteractionCandidates(eq(1L), any()); // bot1 interaction
        order.verify(candidateQueryService).findInteractionCandidates(eq(2L), any()); // bot2 interaction
    }

    @Test
    void 품질_미달_게시글은_저장하지_않고_실패로_기록하며_나머지는_저장한다() {
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
        assertThat(outcome.targetCount()).isEqualTo(2);
        assertThat(outcome.failures()).hasSize(1);
        verify(persistenceService, never()).saveBotPost(eq(1L), eq(tooLongTitle));
    }

    @Test
    void 저장_중_예외가_발생해도_다른_항목_저장은_계속된다() {
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
