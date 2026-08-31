package com.valanse.valanse.controller;

import com.valanse.valanse.service.ContentSeedService.ContentSeedOrchestrator;
import com.valanse.valanse.service.ContentSeedService.ContentSeedRunResult;
import com.valanse.valanse.service.ContentSeedService.ContentSeedScheduler;
import com.valanse.valanse.service.ContentSeedService.ContentSeedUsageSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// PR4-2의 핵심 규칙("자동 스케줄과 같은 이름의 LockProvider 락을 직접 획득") 이
// 실제로 자동 스케줄(@SchedulerLock 애노테이션 방식)과 관리자 수동 실행(직접 LockProvider
// 호출 방식)이 같은 DB 락 행을 두고 경쟁하는지 검증한다. 두 경로가 서로 다른 락 획득
// 메커니즘을 쓰기 때문에, 이름만 같다고 실제로 상호 배제가 되는지는 별도로 확인해야 한다.
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"content-seed.enabled=true", "anthropic.api-key=test-key"})
class ContentSeedAdminSchedulerLockIntegrationTest {
    @Autowired private ContentSeedScheduler scheduler;
    @Autowired private ContentSeedAdminController adminController;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockBean private ContentSeedOrchestrator orchestrator;

    @BeforeEach
    void createLockTable() {
        jdbcTemplate.execute("""
                create table if not exists shedlock (
                    name varchar(64) not null primary key,
                    lock_until timestamp(3) not null,
                    locked_at timestamp(3) not null,
                    locked_by varchar(255) not null
                )
                """);
        jdbcTemplate.update("delete from shedlock");
    }

    @Test
    void 자동_스케줄이_실행_중이면_관리자_수동_실행은_409를_반환한다() throws Exception {
        CountDownLatch schedulerStarted = new CountDownLatch(1);
        CountDownLatch releaseScheduler = new CountDownLatch(1);
        when(orchestrator.run()).thenAnswer(invocation -> {
            schedulerStarted.countDown();
            assertThat(releaseScheduler.await(5, TimeUnit.SECONDS)).isTrue();
            return new ContentSeedRunResult(List.of(), List.of(), ContentSeedUsageSummary.empty(), List.of());
        });

        CompletableFuture<Void> schedulerRun = CompletableFuture.runAsync(scheduler::run);
        assertThat(schedulerStarted.await(5, TimeUnit.SECONDS)).isTrue();

        ResponseEntity<Map<String, Object>> response = adminController.run();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        releaseScheduler.countDown();
        schedulerRun.get(5, TimeUnit.SECONDS);
    }
}
