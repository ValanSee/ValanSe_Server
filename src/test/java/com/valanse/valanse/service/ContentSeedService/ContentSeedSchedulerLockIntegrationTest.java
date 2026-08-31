package com.valanse.valanse.service.ContentSeedService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// PR4-1의 핵심 규칙(자동 스케줄이 전용 @SchedulerLock으로 중복 실행을 막는다)이
// 실제 DB 락으로도 지켜지는지 검증한다. SoftDeletePurgeSchedulerLockIntegrationTest와
// 동일한 방식(shedlock 테이블을 직접 만들고 두 인스턴스를 경쟁시킴)을 따른다.
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "content-seed.enabled=true")
class ContentSeedSchedulerLockIntegrationTest {
    @Autowired private ContentSeedScheduler scheduler;
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
    void 두_인스턴스가_같은_락을_두고_경쟁하면_한_번만_실행된다() throws Exception {
        CountDownLatch firstExecutionStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstExecution = new CountDownLatch(1);
        when(orchestrator.run()).thenAnswer(invocation -> {
            firstExecutionStarted.countDown();
            assertThat(releaseFirstExecution.await(5, TimeUnit.SECONDS)).isTrue();
            return new ContentSeedRunResult(List.of(), List.of(), ContentSeedUsageSummary.empty(), List.of());
        });

        CompletableFuture<Void> first = CompletableFuture.runAsync(scheduler::run);
        assertThat(firstExecutionStarted.await(5, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Void> competing = CompletableFuture.runAsync(scheduler::run);
        competing.get(5, TimeUnit.SECONDS);
        releaseFirstExecution.countDown();
        first.get(5, TimeUnit.SECONDS);

        verify(orchestrator).run();
    }
}
