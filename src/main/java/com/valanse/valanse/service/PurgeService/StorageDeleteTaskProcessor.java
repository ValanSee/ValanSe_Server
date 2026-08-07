package com.valanse.valanse.service.PurgeService;

import com.valanse.valanse.service.StorageService.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageDeleteTaskProcessor {
    private final StorageDeleteTaskRepository taskRepository;
    private final StorageService storageService;
    private final StorageDeleteProperties properties;

    public int processBatch(LocalDateTime now) {
        List<ClaimedStorageDeleteTask> tasks = taskRepository.claimBatch(
                now,
                properties.batchSize(),
                Duration.ofSeconds(properties.leaseSeconds()));

        for (ClaimedStorageDeleteTask task : tasks) {
            process(task, now);
        }
        return tasks.size();
    }

    private void process(ClaimedStorageDeleteTask task, LocalDateTime now) {
        try {
            storageService.deleteImageByUrl(task.objectUrl());
            if (!taskRepository.markCompleted(task, now)) {
                log.warn("Storage delete task completion ignored because its lease was lost. taskId={}", task.id());
            }
        } catch (RuntimeException exception) {
            LocalDateTime nextAttemptAt = now.plusSeconds(retryDelaySeconds(task.attemptCount()));
            String error = exception.getClass().getSimpleName() + ": " + safeMessage(exception);
            if (!taskRepository.markFailed(
                    task,
                    now,
                    nextAttemptAt,
                    properties.maxAttempts(),
                    error)) {
                log.warn("Storage delete task failure ignored because its lease was lost. taskId={}", task.id());
            }
            log.warn(
                    "Storage delete task failed. taskId={}, attempt={}, maxAttempts={}, nextAttemptAt={}",
                    task.id(),
                    task.attemptCount(),
                    properties.maxAttempts(),
                    nextAttemptAt);
        }
    }

    private long retryDelaySeconds(int attemptCount) {
        int exponent = Math.min(Math.max(attemptCount - 1, 0), 30);
        long multiplier = 1L << exponent;
        long initialDelay = properties.initialRetrySeconds();
        long uncappedDelay;
        try {
            uncappedDelay = Math.multiplyExact(initialDelay, multiplier);
        } catch (ArithmeticException exception) {
            uncappedDelay = Long.MAX_VALUE;
        }
        return Math.min(uncappedDelay, properties.maxRetrySeconds());
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "no message" : exception.getMessage();
    }
}
