package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.alert.ContentSeedRunEvent;
import com.valanse.valanse.common.config.ContentSeedProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentSeedRunnerTest {

    @Test
    void 실행이_성공하면_완료_이벤트를_발행한다() {
        ContentSeedOrchestrator orchestrator = mock(ContentSeedOrchestrator.class);
        ContentSeedProperties properties = new ContentSeedProperties();
        properties.setFrontendBaseUrl("https://valanse.kr");
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ContentSeedRunner runner = new ContentSeedRunner(orchestrator, properties, eventPublisher);

        ContentSeedRunResult result = new ContentSeedRunResult(
                List.of(new ContentSeedBatchOutcome(1L, 1, 1, List.of())),
                List.of(new ContentSeedBatchOutcome(1L, 1, 1, List.of())),
                new ContentSeedUsageSummary(1, 100, 50, BigDecimal.ZERO),
                List.of(100L));
        when(orchestrator.run()).thenReturn(result);

        runner.runAndNotify("SCHEDULED");

        ArgumentCaptor<ContentSeedRunEvent> captor = ArgumentCaptor.forClass(ContentSeedRunEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ContentSeedRunEvent event = captor.getValue();
        assertThat(event.isFatal()).isFalse();
        assertThat(event.trigger()).isEqualTo("SCHEDULED");
        assertThat(event.result()).isEqualTo(result);
        assertThat(event.frontendBaseUrl()).isEqualTo("https://valanse.kr");
    }

    @Test
    void 실행이_예외로_실패하면_치명적_오류_이벤트를_발행하고_예외를_다시_던진다() {
        ContentSeedOrchestrator orchestrator = mock(ContentSeedOrchestrator.class);
        ContentSeedProperties properties = new ContentSeedProperties();
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ContentSeedRunner runner = new ContentSeedRunner(orchestrator, properties, eventPublisher);
        when(orchestrator.run()).thenThrow(new IllegalStateException("API Key 없음"));

        assertThatThrownBy(() -> runner.runAndNotify("ADMIN"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("API Key 없음");

        ArgumentCaptor<ContentSeedRunEvent> captor = ArgumentCaptor.forClass(ContentSeedRunEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ContentSeedRunEvent event = captor.getValue();
        assertThat(event.isFatal()).isTrue();
        assertThat(event.trigger()).isEqualTo("ADMIN");
        assertThat(event.fatalErrorType()).isEqualTo("IllegalStateException");
        assertThat(event.fatalErrorMessage()).isEqualTo("API Key 없음");
    }

    @Test
    void 활성_프로필이_없으면_default로_표시한다() {
        ContentSeedOrchestrator orchestrator = mock(ContentSeedOrchestrator.class);
        ContentSeedProperties properties = new ContentSeedProperties();
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ContentSeedRunner runner = new ContentSeedRunner(orchestrator, properties, eventPublisher);
        ReflectionTestUtils.setField(runner, "activeProfiles", "");
        when(orchestrator.run()).thenReturn(new ContentSeedRunResult(
                List.of(), List.of(), ContentSeedUsageSummary.empty(), List.of()));

        runner.runAndNotify("SCHEDULED");

        ArgumentCaptor<ContentSeedRunEvent> captor = ArgumentCaptor.forClass(ContentSeedRunEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().environment()).isEqualTo("default");
    }
}
