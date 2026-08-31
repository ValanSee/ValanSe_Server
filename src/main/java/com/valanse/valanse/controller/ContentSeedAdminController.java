package com.valanse.valanse.controller;

import com.valanse.valanse.common.config.AnthropicProperties;
import com.valanse.valanse.common.config.ContentSeedAdminConfig;
import com.valanse.valanse.service.ContentSeedService.ContentSeedRunner;
import com.valanse.valanse.service.ContentSeedService.ContentSeedScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

// 관리자가 콘텐츠 시드 생성을 즉시 수동으로 실행하는 API. 자동 스케줄
// (ContentSeedScheduler)과 같은 이름의 LockProvider 락을 직접 획득해 중복 실행을
// 막는다 - @SchedulerLock 애노테이션은 자동 스케줄에만 붙어 있어, 수동 실행에서
// "이미 실행 중이면 409"를 즉시 응답하려면 이 컨트롤러가 직접 락을 시도해야 한다.
@Tag(name = "관리자 콘텐츠 시드 API", description = "관리자 전용 콘텐츠 시드 수동 실행 API")
@RestController
@RequestMapping("/admin/content-seed")
public class ContentSeedAdminController {

    private static final Logger log = LoggerFactory.getLogger(ContentSeedAdminController.class);

    private final LockProvider lockProvider;
    private final ContentSeedRunner runner;
    private final AnthropicProperties anthropicProperties;
    private final ExecutorService executor;

    @Value("${content-seed.lock-at-most-for:PT2H}")
    private Duration lockAtMostFor;

    @Value("${content-seed.lock-at-least-for:PT0S}")
    private Duration lockAtLeastFor;

    public ContentSeedAdminController(
            LockProvider lockProvider,
            ContentSeedRunner runner,
            AnthropicProperties anthropicProperties,
            @Qualifier(ContentSeedAdminConfig.ADMIN_EXECUTOR) ExecutorService executor
    ) {
        this.lockProvider = lockProvider;
        this.runner = runner;
        this.anthropicProperties = anthropicProperties;
        this.executor = executor;
    }

    @PostMapping("/run")
    @Operation(
            summary = "콘텐츠 시드 수동 실행",
            description = "콘텐츠 시드 생성을 즉시 비동기로 실행합니다. 자동 스케줄과 같은 락을 사용하므로 "
                    + "이미 실행 중이면 409를 반환합니다."
    )
    public ResponseEntity<Map<String, Object>> run() {
        if (!StringUtils.hasText(anthropicProperties.getApiKey())) {
            return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Anthropic API Key가 설정되어 있지 않습니다.");
        }

        Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
                Instant.now(), ContentSeedScheduler.LOCK_NAME, lockAtMostFor, lockAtLeastFor));
        if (lock.isEmpty()) {
            return errorResponse(HttpStatus.CONFLICT, "콘텐츠 시드가 이미 실행 중입니다.");
        }

        SimpleLock acquiredLock = lock.get();
        executor.execute(() -> {
            try {
                runner.runAndNotify("ADMIN");
            } catch (RuntimeException e) {
                // ContentSeedRunner가 이미 로그·Discord 알림을 남겼으므로 여기서는 조용히 흡수한다.
                log.debug("Admin content seed run finished with an exception already handled.", e);
            } finally {
                acquiredLock.unlock();
            }
        });

        Map<String, Object> body = new HashMap<>();
        body.put("message", "콘텐츠 시드 실행을 시작했습니다.");
        return ResponseEntity.accepted().body(body);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }
}
