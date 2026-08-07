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
        long startedAt = System.nanoTime();
        LocalDateTime startedNow = LocalDateTime.now();
        LocalDateTime cutoff = startedNow.minusDays(properties.retentionDays());
        int comments = 0;
        int votes = 0;
        int members = 0;
        int batches = 0;
        log.info("PURGE_STARTED cutoff={} retentionDays={} batchSize={}",
                cutoff, properties.retentionDays(), properties.batchSize());
        try {
            while (true) {
                PurgeResult result = purgeService.purgeExpired(LocalDateTime.now());
                batches++;
                comments += result.commentsAnonymized();
                votes += result.votesDeleted();
                members += result.membersDeleted();
                if (result.commentsAnonymized() == 0 && result.votesDeleted() == 0 && result.membersDeleted() == 0) break;
            }
            log.info("PURGE_COMPLETED batches={} comments={} votes={} members={} durationMs={}",
                    batches, comments, votes, members, elapsedMillis(startedAt));
        } catch (RuntimeException e) {
            log.error("PURGE_FAILED batches={} comments={} votes={} members={} durationMs={}",
                    batches, comments, votes, members, elapsedMillis(startedAt), e);
            throw e;
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
