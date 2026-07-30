package com.valanse.valanse.common.alert;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
@EnableConfigurationProperties(DiscordAlertProperties.class)
public class DiscordAlertConfiguration {

    public static final String ALERT_EXECUTOR = "discordAlertExecutor";

    @Bean(name = ALERT_EXECUTOR)
    public TaskExecutor discordAlertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("discord-alert-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }

    @Bean
    public RestClient discordRestClient(DiscordAlertProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("User-Agent", "ValanSe-Server-Error-Alert");
                    return execution.execute(request, body);
                })
                .build();
    }

    @Bean
    public DiscordWebhookClient discordWebhookClient(
            @Qualifier("discordRestClient") RestClient restClient,
            DiscordAlertProperties properties
    ) {
        return new DiscordWebhookClient(restClient, properties);
    }
}
