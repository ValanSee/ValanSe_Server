package com.valanse.valanse.common.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "alert.discord", name = "enabled", havingValue = "true")
public class DailyVoteEventListener {

    private static final Logger log = LoggerFactory.getLogger(DailyVoteEventListener.class);

    private final DiscordWebhookClient webhookClient;

    public DailyVoteEventListener(DiscordWebhookClient webhookClient) {
        this.webhookClient = webhookClient;
    }

    @Async(DiscordAlertConfiguration.ALERT_EXECUTOR)
    @EventListener
    public void onDailyVote(DailyVoteEvent event) {
        try {
            webhookClient.send(event);
        } catch (Exception exception) {
            // RestClient 예외에는 토큰이 포함된 웹훅 URL이 들어갈 수 있으므로 예외 본문은 기록하지 않습니다.
            log.error(
                    "Failed to send Discord daily vote alert. type={}, date={}, failureType={}",
                    event.type(),
                    event.date(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
