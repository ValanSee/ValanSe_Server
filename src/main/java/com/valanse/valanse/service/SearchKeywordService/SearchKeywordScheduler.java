package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.common.config.SearchKeywordProperties;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 검색 키워드를 수집하는 스케줄러. LOCK_NAME은 관리자 수동 수집 API가
// 같은 이름으로 LockProvider 락을 직접 획득할 때도 사용한다.
@Component
@RequiredArgsConstructor
public class SearchKeywordScheduler {

    public static final String LOCK_NAME = "searchKeywordCollect";

    private final SearchKeywordCollector collector;
    private final SearchKeywordProperties properties;

    @Scheduled(cron = "${search-keyword.cron:0 0 3 * * *}", zone = "${search-keyword.zone:Asia/Seoul}")
    @SchedulerLock(name = LOCK_NAME, lockAtMostFor = "PT30M")
    public void run() {
        if (!properties.isEnabled()) return;
        collector.collect();
    }
}
