package com.valanse.valanse.service.PurgeService;

import com.valanse.valanse.service.StorageService.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SoftDeletePurgeServiceTest {
    private JdbcTemplate jdbcTemplate;
    private StorageService storageService;
    private SoftDeletePurgeService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        storageService = mock(StorageService.class);
        service = new SoftDeletePurgeService(jdbcTemplate, storageService, new PurgeProperties(true, 14, 100));
    }

    @Test
    void expiredCommentIsAnonymizedOnlyOnceAndStructureIsKept() {
        when(jdbcTemplate.queryForList(startsWith("select id from comment"), eq(Long.class), any(Timestamp.class), eq(100)))
                .thenReturn(List.of(7L));
        when(jdbcTemplate.queryForList(startsWith("select id from vote"), eq(Long.class), any(Timestamp.class), eq(100)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList(startsWith("select id from member"), eq(Long.class), any(Timestamp.class), eq(100)))
                .thenReturn(List.of());

        PurgeResult result = service.purgeExpired(LocalDateTime.of(2026, 7, 8, 4, 0));

        assertThat(result).isEqualTo(new PurgeResult(1, 0, 0));
        verify(jdbcTemplate).update("delete from comment_like where comment_id = ?", 7L);
        verify(jdbcTemplate).update("delete from report where report_type = 'COMMENT' and target_id = ?", 7L);
        verify(jdbcTemplate).update(contains("purged_at = current_timestamp"), eq(7L));
        verify(jdbcTemplate, never()).update(startsWith("delete from comment where id"), (Object[]) any());
    }

    @Test
    void cutoffIsExactlyFourteenDaysBeforeExecutionTime() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(Timestamp.class), eq(100)))
                .thenReturn(List.of());

        service.purgeExpired(LocalDateTime.of(2026, 7, 8, 4, 0));

        verify(jdbcTemplate, times(3)).queryForList(anyString(), eq(Long.class),
                eq(Timestamp.valueOf(LocalDateTime.of(2026, 6, 24, 4, 0))), eq(100));
    }
}
