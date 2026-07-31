package com.valanse.valanse.service.PurgeService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;

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

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
