package com.valanse.valanse.service.PurgeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class SoftDeletePurgeSchedulerLockIntegrationTest {
    @Autowired private SoftDeletePurgeScheduler scheduler;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockBean private SoftDeletePurgeService purgeService;

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
    void onlyOnePurgeRunsWhenTwoInstancesCompeteForTheSameDatabaseLock() throws Exception {
        CountDownLatch firstExecutionStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstExecution = new CountDownLatch(1);
        when(purgeService.purgeExpired(any())).thenAnswer(invocation -> {
            firstExecutionStarted.countDown();
            assertThat(releaseFirstExecution.await(5, TimeUnit.SECONDS)).isTrue();
            return new PurgeResult(0, 0, 0);
        });

        CompletableFuture<Void> first = CompletableFuture.runAsync(scheduler::purge);
        assertThat(firstExecutionStarted.await(5, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Void> competing = CompletableFuture.runAsync(scheduler::purge);
        competing.get(5, TimeUnit.SECONDS);
        releaseFirstExecution.countDown();
        first.get(5, TimeUnit.SECONDS);

        verify(purgeService).purgeExpired(any());
    }
}
