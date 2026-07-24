package com.valanse.valanse.common.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DiscordServerErrorListenerTest {

    @Test
    @DisplayName("서버 오류 이벤트를 Discord 클라이언트에 전달한다")
    void onServerError_SendsWebhook() {
        DiscordWebhookClient client = mock(DiscordWebhookClient.class);
        DiscordServerErrorListener listener = new DiscordServerErrorListener(client);
        ServerErrorEvent event = event();

        listener.onServerError(event);

        verify(client).send(event);
    }

    @Test
    @DisplayName("Discord 전송 실패가 이벤트 처리 밖으로 전파되지 않는다")
    void onServerError_WhenWebhookFails_DoesNotPropagate() {
        DiscordWebhookClient client = mock(DiscordWebhookClient.class);
        DiscordServerErrorListener listener = new DiscordServerErrorListener(client);
        ServerErrorEvent event = event();
        doThrow(new RuntimeException("discord unavailable")).when(client).send(event);

        assertThatCode(() -> listener.onServerError(event)).doesNotThrowAnyException();
    }

    private ServerErrorEvent event() {
        return new ServerErrorEvent(
                Instant.now(), "test", 500, "GET", "/boom",
                "RuntimeException", "boom", "trace-1", new RuntimeException("boom")
        );
    }
}
