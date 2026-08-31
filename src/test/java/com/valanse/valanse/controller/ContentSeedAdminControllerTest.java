package com.valanse.valanse.controller;

import com.valanse.valanse.common.config.AnthropicProperties;
import com.valanse.valanse.service.ContentSeedService.ContentSeedRunner;
import com.valanse.valanse.service.ContentSeedService.ContentSeedScheduler;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContentSeedAdminControllerTest {

    private LockProvider lockProvider;
    private ContentSeedRunner runner;
    private AnthropicProperties anthropicProperties;
    private ExecutorService executor;
    private ContentSeedAdminController controller;

    @BeforeEach
    void setUp() {
        lockProvider = mock(LockProvider.class);
        runner = mock(ContentSeedRunner.class);
        anthropicProperties = new AnthropicProperties();
        anthropicProperties.setApiKey("test-api-key");
        executor = mock(ExecutorService.class);
        // execute()가 호출되면 넘겨받은 Runnable을 테스트 스레드에서 즉시 동기 실행한다 -
        // 이렇게 하면 비동기 실행 여부를 기다리지 않고도 결정적으로 검증할 수 있다.
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any());

        controller = new ContentSeedAdminController(lockProvider, runner, anthropicProperties, executor);
        ReflectionTestUtils.setField(controller, "lockAtMostFor", Duration.ofHours(2));
        ReflectionTestUtils.setField(controller, "lockAtLeastFor", Duration.ZERO);
    }

    @Test
    void API_Key가_없으면_503을_반환하고_아무것도_실행하지_않는다() {
        anthropicProperties.setApiKey("");

        ResponseEntity<Map<String, Object>> response = controller.run();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(lockProvider);
        verifyNoInteractions(executor);
    }

    @Test
    void 락_획득에_실패하면_409를_반환하고_실행하지_않는다() {
        when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.run();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(executor);
        verify(runner, never()).runAndNotify(any());
    }

    @Test
    void 락_획득에_성공하면_202를_반환하고_비동기로_실행한_뒤_락을_해제한다() {
        SimpleLock simpleLock = mock(SimpleLock.class);
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);
        when(lockProvider.lock(configCaptor.capture())).thenReturn(Optional.of(simpleLock));

        ResponseEntity<Map<String, Object>> response = controller.run();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(configCaptor.getValue().getName()).isEqualTo(ContentSeedScheduler.LOCK_NAME);
        verify(runner).runAndNotify("ADMIN");
        verify(simpleLock).unlock();
    }

    @Test
    void 실행_중_예외가_발생해도_락은_해제된다() {
        SimpleLock simpleLock = mock(SimpleLock.class);
        when(lockProvider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(simpleLock));
        doAnswer(invocation -> { throw new IllegalStateException("boom"); })
                .when(runner).runAndNotify("ADMIN");

        ResponseEntity<Map<String, Object>> response = controller.run();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(simpleLock).unlock();
    }
}
