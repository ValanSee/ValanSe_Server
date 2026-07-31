package com.valanse.valanse.domain;

import com.valanse.valanse.domain.enums.StorageDeleteTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "storage_delete_task",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_storage_delete_task_deduplication",
                columnNames = "deduplication_key"
        )
)
@Getter
@NoArgsConstructor
public class StorageDeleteTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deduplication_key", nullable = false, length = 64)
    private String deduplicationKey;

    @Column(name = "object_url", nullable = false, length = 2048)
    private String objectUrl;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StorageDeleteTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "lock_token", length = 36)
    private String lockToken;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
