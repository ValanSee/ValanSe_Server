package com.valanse.valanse.service.PurgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(PurgeProperties.class)
public class SoftDeletePurgeScheduler {
    private final SoftDeletePurgeService purgeService;
    private final PurgeProperties properties;

    @Scheduled(cron = "${purge.cron:0 0 4 * * *}", zone = "${purge.zone:Asia/Seoul}")
    @SchedulerLock(
            name = "softDeletePurge",
            lockAtMostFor = "${purge.lock-at-most-for:PT30M}",
            lockAtLeastFor = "${purge.lock-at-least-for:PT0S}"
    )
    public void purge() {
        if (!properties.enabled()) return;
        int comments = 0;
        int votes = 0;
        int members = 0;
        while (true) {
            PurgeResult result = purgeService.purgeExpired(LocalDateTime.now());
            comments += result.commentsAnonymized();
            votes += result.votesDeleted();
            members += result.membersDeleted();
            if (result.commentsAnonymized() == 0 && result.votesDeleted() == 0 && result.membersDeleted() == 0) break;
        }
        log.info("Soft-delete purge completed: comments={}, votes={}, members={}", comments, votes, members);
    }
}
