package com.valanse.valanse.service.PurgeService;

public record ClaimedStorageDeleteTask(
        long id,
        String objectUrl,
        String lockToken,
        int attemptCount
) {
}
