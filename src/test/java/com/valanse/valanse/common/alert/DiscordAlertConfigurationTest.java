package com.valanse.valanse.common.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordAlertConfigurationTest {

    @Test
    @DisplayName("Discord 알림 Executor는 고정 스레드 2개와 대기 큐 50개를 사용한다")
    void discordAlertExecutor_UsesExpectedPoolSizeAndQueueCapacity() {
        DiscordAlertConfiguration configuration = new DiscordAlertConfiguration();

        TaskExecutor taskExecutor = configuration.discordAlertExecutor();

        assertThat(taskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(2);
        assertThat(executor.getQueueCapacity()).isEqualTo(50);
    }
}
