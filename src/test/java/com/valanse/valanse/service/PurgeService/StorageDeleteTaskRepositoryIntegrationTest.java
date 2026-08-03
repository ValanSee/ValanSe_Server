package com.valanse.valanse.service.PurgeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StorageDeleteTaskRepositoryIntegrationTest {
    @Autowired private StorageDeleteTaskRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTasks() {
        jdbcTemplate.update("delete from storage_delete_task");
    }

    @Test
    void duplicateObjectIsEnqueuedOnlyOnce() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 4, 0);

        repository.enqueue("https://cdn.example.com/a.png", "VOTE", 1L, now);
        repository.enqueue("https://cdn.example.com/a.png", "VOTE", 1L, now);

        assertThat(taskCount()).isEqualTo(1);
    }

    @Test
    void claimedTaskCannotBeClaimedAgainUntilLeaseExpires() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 4, 0);
        repository.enqueue("https://cdn.example.com/a.png", "VOTE", 1L, now);

        List<ClaimedStorageDeleteTask> first = repository.claimBatch(now, 10, Duration.ofMinutes(5));
        List<ClaimedStorageDeleteTask> second = repository.claimBatch(now.plusMinutes(1), 10, Duration.ofMinutes(5));
        List<ClaimedStorageDeleteTask> recovered = repository.claimBatch(now.plusMinutes(6), 10, Duration.ofMinutes(5));

        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();
        assertThat(recovered).hasSize(1);
        assertThat(recovered.get(0).attemptCount()).isEqualTo(2);
    }

    @Test
    void failedTaskIsRetriedAndEventuallyMovedToDead() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 4, 0);
        repository.enqueue("https://cdn.example.com/a.png", "VOTE", 1L, now);
        ClaimedStorageDeleteTask first = repository.claimBatch(now, 10, Duration.ofMinutes(5)).get(0);

        repository.markFailed(first, now, now.plusMinutes(1), 2, "temporary failure");
        assertThat(status()).isEqualTo("RETRY");
        assertThat(repository.claimBatch(now.plusSeconds(30), 10, Duration.ofMinutes(5))).isEmpty();

        ClaimedStorageDeleteTask second = repository.claimBatch(
                now.plusMinutes(1), 10, Duration.ofMinutes(5)).get(0);
        repository.markFailed(second, now.plusMinutes(1), now.plusMinutes(3), 2, "permanent failure");

        assertThat(status()).isEqualTo("DEAD");
        assertThat(repository.retryDead(second.id(), now.plusMinutes(4))).isEqualTo(1);
        assertThat(status()).isEqualTo("RETRY");
    }

    @Test
    void completedTaskIsNeverClaimedAgain() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 4, 0);
        repository.enqueue("https://cdn.example.com/a.png", "VOTE", 1L, now);
        ClaimedStorageDeleteTask task = repository.claimBatch(now, 10, Duration.ofMinutes(5)).get(0);

        repository.markCompleted(task, now);

        assertThat(status()).isEqualTo("COMPLETED");
        assertThat(repository.claimBatch(now.plusDays(1), 10, Duration.ofMinutes(5))).isEmpty();
    }

    private int taskCount() {
        return jdbcTemplate.queryForObject("select count(*) from storage_delete_task", Integer.class);
    }

    private String status() {
        return jdbcTemplate.queryForObject("select status from storage_delete_task", String.class);
    }
}
