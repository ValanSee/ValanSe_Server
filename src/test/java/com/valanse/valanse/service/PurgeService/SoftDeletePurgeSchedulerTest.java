package com.valanse.valanse.service.PurgeService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SoftDeletePurgeSchedulerTest {
    @Test
    void repeatsBatchesUntilNoExpiredRowsRemain() {
        SoftDeletePurgeService service = mock(SoftDeletePurgeService.class);
        when(service.purgeExpired(any()))
                .thenReturn(new PurgeResult(100, 0, 0))
                .thenReturn(new PurgeResult(1, 0, 0))
                .thenReturn(new PurgeResult(0, 0, 0));
        SoftDeletePurgeScheduler scheduler = new SoftDeletePurgeScheduler(
                service, new PurgeProperties(true, 14, 100));

        scheduler.purge();

        verify(service, times(3)).purgeExpired(any());
    }

    @Test
    void doesNothingWhenDisabled() {
        SoftDeletePurgeService service = mock(SoftDeletePurgeService.class);
        SoftDeletePurgeScheduler scheduler = new SoftDeletePurgeScheduler(
                service, new PurgeProperties(false, 14, 100));

        scheduler.purge();

        verifyNoInteractions(service);
    }

    @Test
    void propagatesFailureForSchedulerErrorHandlerAndReleasesRunningFlag() {
        SoftDeletePurgeService service = mock(SoftDeletePurgeService.class);
        when(service.purgeExpired(any()))
                .thenThrow(new IllegalStateException("purge failed"))
                .thenReturn(new PurgeResult(0, 0, 0));
        SoftDeletePurgeScheduler scheduler = new SoftDeletePurgeScheduler(
                service, new PurgeProperties(true, 14, 100));

        assertThrows(IllegalStateException.class, scheduler::purge);
        scheduler.purge();

        verify(service, times(2)).purgeExpired(any());
    }
}
