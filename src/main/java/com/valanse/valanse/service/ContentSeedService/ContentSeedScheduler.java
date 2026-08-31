package com.valanse.valanse.service.ContentSeedService;

import com.valanse.valanse.common.config.ContentSeedProperties;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매주 콘텐츠 시드를 자동 실행하는 스케줄러. LOCK_NAME은 관리자 수동 실행(PR4-2)이
// 같은 이름으로 LockProvider 락을 직접 획득할 때도 사용해, 자동 실행과 수동 실행이
// 동시에 겹치지 않도록 한다.
@Component
@RequiredArgsConstructor
public class ContentSeedScheduler {

    public static final String LOCK_NAME = "contentSeedRun";

    private final ContentSeedRunner runner;
    private final ContentSeedProperties properties;

    @Scheduled(cron = "${content-seed.cron:0 0 4 * * MON}", zone = "${content-seed.zone:Asia/Seoul}")
    @SchedulerLock(
            name = LOCK_NAME,
            lockAtMostFor = "${content-seed.lock-at-most-for:PT2H}",
            lockAtLeastFor = "${content-seed.lock-at-least-for:PT0S}"
    )
    public void run() {
        if (!properties.isEnabled()) return;
        runner.runAndNotify("SCHEDULED");
    }
}
