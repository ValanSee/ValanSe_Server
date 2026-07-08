package com.valanse.valanse.service.PurgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(PurgeProperties.class)
public class SoftDeletePurgeScheduler {
    private final SoftDeletePurgeService purgeService;
    private final PurgeProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${purge.cron:0 0 4 * * *}", zone = "${purge.zone:Asia/Seoul}")
    public void purge() {
        if (!properties.enabled()) return;
        if (!running.compareAndSet(false, true)) {
            log.info("Soft-delete purge skipped because a previous execution is still running");
            return;
        }
        int comments = 0;
        int votes = 0;
        int members = 0;
        try {
            while (true) {
                PurgeResult result = purgeService.purgeExpired(LocalDateTime.now());
                comments += result.commentsAnonymized();
                votes += result.votesDeleted();
                members += result.membersDeleted();
                if (result.commentsAnonymized() == 0 && result.votesDeleted() == 0 && result.membersDeleted() == 0) break;
            }
            log.info("Soft-delete purge completed: comments={}, votes={}, members={}", comments, votes, members);
        } finally {
            running.set(false);
        }
    }
}
