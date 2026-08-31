package com.valanse.valanse.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 관리자 수동 콘텐츠 시드 실행(PR4-2) 전용 단일 스레드 executor.
// 실행 중복은 락(ContentSeedScheduler.LOCK_NAME)으로 막으므로, 이 executor는
// 그 실행 1건이 요청 스레드를 막지 않고 비동기로 진행되게 하는 역할만 한다.
@Configuration
public class ContentSeedAdminConfig {

    public static final String ADMIN_EXECUTOR = "contentSeedAdminExecutor";

    @Bean(name = ADMIN_EXECUTOR, destroyMethod = "shutdown")
    public ExecutorService contentSeedAdminExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "content-seed-admin");
            thread.setDaemon(true);
            return thread;
        });
    }
}
