package com.valanse.valanse.service.PurgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageDeleteProperties.class)
public class StorageDeleteTaskScheduler {
    private final StorageDeleteTaskProcessor processor;
    private final StorageDeleteProperties properties;

    @Scheduled(fixedDelayString = "${purge.storage-delete.fixed-delay-ms:10000}")
    public void dispatch() {
        if (!properties.enabled()) {
            return;
        }

        int processed = processor.processBatch(LocalDateTime.now());
        if (processed > 0) {
            log.info("Storage delete task batch processed. count={}", processed);
        }
    }
}
