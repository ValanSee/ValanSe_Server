package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.alert.ContentSeedRunEvent;
import com.valanse.valanse.common.config.ContentSeedProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

// 주간 자동 스케줄과 관리자 수동 실행이 공유하는 실행 본체.
// 오케스트레이터를 호출하고, 결과(또는 치명적 오류)를 로그와 Discord 알림으로 남긴다.
// 잠금(락) 획득/해제는 이 클래스의 책임이 아니다 - 호출자(스케줄러의 @SchedulerLock,
// 관리자 API의 직접 LockProvider 획득)가 각자의 방식으로 처리한다.
@Slf4j
@Component
public class ContentSeedRunner {

    private final ContentSeedOrchestrator orchestrator;
    private final ContentSeedProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public ContentSeedRunner(
            ContentSeedOrchestrator orchestrator,
            ContentSeedProperties properties,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orchestrator = orchestrator;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    public void runAndNotify(String trigger) {
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        log.info("CONTENT_SEED_STARTED trigger={}", trigger);

        try {
            ContentSeedRunResult result = orchestrator.run();
            long durationMs = elapsedMillis(startedNanos);
            log.info(
                    "CONTENT_SEED_COMPLETED trigger={} posts={}/{} interactions={}/{} durationMs={}",
                    trigger,
                    result.savedPostCount(), result.targetPostCount(),
                    result.savedInteractionCount(), result.targetInteractionCount(),
                    durationMs
            );
            eventPublisher.publishEvent(ContentSeedRunEvent.completed(
                    activeProfile(), trigger, startedAt, durationMs, result, properties.getFrontendBaseUrl()));
        } catch (RuntimeException e) {
            long durationMs = elapsedMillis(startedNanos);
            log.error("CONTENT_SEED_FAILED trigger={} durationMs={}", trigger, durationMs, e);
            eventPublisher.publishEvent(ContentSeedRunEvent.fatal(activeProfile(), trigger, startedAt, durationMs, e));
            throw e;
        }
    }

    private String activeProfile() {
        return activeProfiles == null || activeProfiles.isBlank() ? "default" : activeProfiles;
    }

    private long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
