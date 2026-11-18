package com.valanse.valanse.controller;

import com.valanse.valanse.service.SearchKeywordService.SearchKeywordCollectResult;
import com.valanse.valanse.service.SearchKeywordService.SearchKeywordCollector;
import com.valanse.valanse.service.SearchKeywordService.SearchKeywordScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

// 관리자가 검색 키워드 수집을 즉시 동기 실행하는 API(운영 수동 확인용).
// 자동 스케줄과 같은 락을 잡아 중복 실행을 막고, enabled 설정과 무관하게 실행한다.
@Tag(name = "관리자 검색 키워드 API", description = "관리자 전용 검색 키워드 수동 수집 API")
@RestController
@RequestMapping("/admin/search-keywords")
@RequiredArgsConstructor
public class SearchKeywordAdminController {

    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(30);

    private final LockProvider lockProvider;
    private final SearchKeywordCollector collector;

    @PostMapping("/collect")
    @Operation(
            summary = "검색 키워드 수동 수집",
            description = "검색 키워드를 즉시 동기로 수집합니다. 자동 스케줄과 같은 락을 사용하므로 "
                    + "이미 실행 중이면 409를 반환합니다."
    )
    public ResponseEntity<?> collect() {
        Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
                Instant.now(), SearchKeywordScheduler.LOCK_NAME, LOCK_AT_MOST_FOR, Duration.ZERO));
        if (lock.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "검색 키워드 수집이 이미 실행 중입니다."));
        }
        try {
            SearchKeywordCollectResult result = collector.collect();
            return ResponseEntity.ok(Map.of(
                    "savedCounts", result.savedCounts(),
                    "failures", result.failures()));
        } finally {
            lock.get().unlock();
        }
    }
}
