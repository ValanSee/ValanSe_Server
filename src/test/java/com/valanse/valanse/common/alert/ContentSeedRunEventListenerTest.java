package com.valanse.valanse.common.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContentSeedRunEventListenerTest {

    @Test
    @DisplayName("콘텐츠 시드 실행 결과 이벤트를 Discord 클라이언트에 전달한다")
    void onContentSeedRun_SendsWebhook() {
        DiscordWebhookClient client = mock(DiscordWebhookClient.class);
        ContentSeedRunEventListener listener = new ContentSeedRunEventListener(client);
        ContentSeedRunEvent event = fatalEvent();

        listener.onContentSeedRun(event);

        verify(client).send(event);
    }

    @Test
    @DisplayName("Discord 전송 실패가 이벤트 처리 밖으로 전파되지 않는다")
    void onContentSeedRun_WhenWebhookFails_DoesNotPropagate() {
        DiscordWebhookClient client = mock(DiscordWebhookClient.class);
        ContentSeedRunEventListener listener = new ContentSeedRunEventListener(client);
        ContentSeedRunEvent event = fatalEvent();
        doThrow(new RuntimeException("discord unavailable")).when(client).send(event);

        assertThatCode(() -> listener.onContentSeedRun(event)).doesNotThrowAnyException();
    }

    private ContentSeedRunEvent fatalEvent() {
        return ContentSeedRunEvent.fatal(
                "test", "SCHEDULED", Instant.now(), 1_000L, new RuntimeException("boom"));
    }
}
