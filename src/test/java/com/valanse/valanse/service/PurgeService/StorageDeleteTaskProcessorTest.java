package com.valanse.valanse.service.PurgeService;

import com.valanse.valanse.service.StorageService.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageDeleteTaskProcessorTest {
    private StorageDeleteTaskRepository repository;
    private StorageService storageService;
    private StorageDeleteTaskProcessor processor;
    private StorageDeleteProperties properties;

    @BeforeEach
    void setUp() {
        repository = mock(StorageDeleteTaskRepository.class);
        storageService = mock(StorageService.class);
        properties = new StorageDeleteProperties();
        processor = new StorageDeleteTaskProcessor(repository, storageService, properties);
    }

    @Test
    void marksClaimedTaskCompletedAfterStorageDeletion() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 4, 0);
        ClaimedStorageDeleteTask task = new ClaimedStorageDeleteTask(
                1L, "https://cdn.example.com/a.png", "token", 1);
        when(repository.claimBatch(now, 50, Duration.ofSeconds(300))).thenReturn(List.of(task));

        int processed = processor.processBatch(now);

        assertThat(processed).isEqualTo(1);
        verify(storageService).deleteImageByUrl(task.objectUrl());
        verify(repository).markCompleted(task, now);
    }

    @Test
    void schedulesFailureWithExponentialBackoff() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 31, 4, 0);
        ClaimedStorageDeleteTask task = new ClaimedStorageDeleteTask(
                2L, "https://cdn.example.com/b.png", "token", 3);
        when(repository.claimBatch(now, 50, Duration.ofSeconds(300))).thenReturn(List.of(task));
        doThrow(new IllegalStateException("R2 unavailable"))
                .when(storageService).deleteImageByUrl(task.objectUrl());

        processor.processBatch(now);

        verify(repository).markFailed(
                task,
                now,
                now.plusSeconds(120),
                8,
                "IllegalStateException: R2 unavailable");
    }
}
