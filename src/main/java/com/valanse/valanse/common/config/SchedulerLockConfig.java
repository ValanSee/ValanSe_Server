package com.valanse.valanse.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerLockConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService shedLockKeepAliveExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "shedlock-keepalive");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    public LockProvider lockProvider(
            JdbcTemplate jdbcTemplate,
            ScheduledExecutorService shedLockKeepAliveExecutor
    ) {
        JdbcTemplateLockProvider jdbcLockProvider = new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(jdbcTemplate)
                        .usingDbTime()
                        .build());
        return new KeepAliveLockProvider(jdbcLockProvider, shedLockKeepAliveExecutor);
    }
}
