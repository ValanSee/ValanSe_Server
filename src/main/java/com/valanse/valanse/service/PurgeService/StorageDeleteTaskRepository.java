package com.valanse.valanse.service.PurgeService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StorageDeleteTaskRepository {
    private final JdbcTemplate jdbcTemplate;

    public void enqueue(String objectUrl, String sourceType, Long sourceId, LocalDateTime now) {
        if (!StringUtils.hasText(objectUrl)) {
            return;
        }

        try {
            jdbcTemplate.update("""
                            insert into storage_delete_task (
                                deduplication_key, object_url, source_type, source_id,
                                status, attempt_count, next_attempt_at, created_at, updated_at
                            ) values (?, ?, ?, ?, 'PENDING', 0, ?, ?, ?)
                            """,
                    sha256(objectUrl),
                    objectUrl,
                    sourceType,
                    sourceId,
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now));
        } catch (DuplicateKeyException ignored) {
            // A committed task for the same immutable object is already durable.
        }
    }

    @Transactional
    public List<ClaimedStorageDeleteTask> claimBatch(LocalDateTime now, int batchSize, Duration leaseDuration) {
        Timestamp timestamp = Timestamp.valueOf(now);
        List<Long> taskIds = jdbcTemplate.queryForList("""
                        select id
                        from storage_delete_task
                        where (
                            status in ('PENDING', 'RETRY') and next_attempt_at <= ?
                        ) or (
                            status = 'PROCESSING' and locked_until < ?
                        )
                        order by id
                        limit ?
                        for update skip locked
                        """,
                Long.class,
                timestamp,
                timestamp,
                batchSize);

        if (taskIds.isEmpty()) {
            return List.of();
        }

        String lockToken = UUID.randomUUID().toString();
        Timestamp lockedUntil = Timestamp.valueOf(now.plus(leaseDuration));
        for (Long taskId : taskIds) {
            jdbcTemplate.update("""
                            update storage_delete_task
                            set status = 'PROCESSING',
                                attempt_count = attempt_count + 1,
                                locked_until = ?,
                                lock_token = ?,
                                updated_at = ?
                            where id = ?
                            """,
                    lockedUntil,
                    lockToken,
                    timestamp,
                    taskId);
        }

        List<ClaimedStorageDeleteTask> tasks = new ArrayList<>(taskIds.size());
        for (Long taskId : taskIds) {
            tasks.add(jdbcTemplate.queryForObject("""
                            select id, object_url, lock_token, attempt_count
                            from storage_delete_task
                            where id = ? and lock_token = ?
                            """,
                    (resultSet, rowNumber) -> new ClaimedStorageDeleteTask(
                            resultSet.getLong("id"),
                            resultSet.getString("object_url"),
                            resultSet.getString("lock_token"),
                            resultSet.getInt("attempt_count")),
                    taskId,
                    lockToken));
        }
        return tasks;
    }

    public boolean markCompleted(ClaimedStorageDeleteTask task, LocalDateTime now) {
        return jdbcTemplate.update("""
                        update storage_delete_task
                        set status = 'COMPLETED',
                            completed_at = ?,
                            locked_until = null,
                            lock_token = null,
                            last_error = null,
                            updated_at = ?
                        where id = ? and status = 'PROCESSING' and lock_token = ?
                        """,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                task.id(),
                task.lockToken()) == 1;
    }

    public boolean markFailed(
            ClaimedStorageDeleteTask task,
            LocalDateTime now,
            LocalDateTime nextAttemptAt,
            int maxAttempts,
            String error
    ) {
        boolean exhausted = task.attemptCount() >= maxAttempts;
        return jdbcTemplate.update("""
                        update storage_delete_task
                        set status = ?,
                            next_attempt_at = ?,
                            locked_until = null,
                            lock_token = null,
                            last_error = ?,
                            updated_at = ?
                        where id = ? and status = 'PROCESSING' and lock_token = ?
                        """,
                exhausted ? "DEAD" : "RETRY",
                Timestamp.valueOf(nextAttemptAt),
                truncate(error, 1000),
                Timestamp.valueOf(now),
                task.id(),
                task.lockToken()) == 1;
    }

    public int retryDead(long taskId, LocalDateTime now) {
        return jdbcTemplate.update("""
                        update storage_delete_task
                        set status = 'RETRY',
                            attempt_count = 0,
                            next_attempt_at = ?,
                            locked_until = null,
                            lock_token = null,
                            completed_at = null,
                            updated_at = ?
                        where id = ? and status = 'DEAD'
                        """,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                taskId);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
