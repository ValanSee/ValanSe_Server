package com.valanse.valanse.common.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

class DiscordWebhookClientTest {

    @Test
    @DisplayName("서버 오류 이벤트를 멘션이 비활성화된 Discord embed로 전송한다")
    void send_PostsDiscordEmbedWithWaitEnabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        DiscordAlertProperties properties = new DiscordAlertProperties();
        properties.setWebhookUrl("https://discord.test/api/webhooks/1/token");
        DiscordWebhookClient client = new DiscordWebhookClient(builder.build(), properties);

        server.expect(once(), requestTo("https://discord.test/api/webhooks/1/token?wait=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "username": "ValanSe Error Alert",
                          "allowed_mentions": {"parse": []},
                          "embeds": [{
                            "title": "Server 500 Error",
                            "fields": [
                              {"name": "Environment", "value": "prod", "inline": true},
                              {"name": "Endpoint", "value": "POST /votes", "inline": false},
                              {"name": "Exception", "value": "IllegalStateException: boom", "inline": false},
                              {"name": "Trace ID", "value": "trace-1", "inline": false}
                            ]
                          }]
                        }
                        """, false))
                .andRespond(withNoContent());

        IllegalStateException cause = new IllegalStateException("boom");
        client.send(new ServerErrorEvent(
                Instant.parse("2026-07-22T06:00:00Z"),
                "prod",
                500,
                "POST",
                "/votes",
                "IllegalStateException",
                "boom",
                "trace-1",
                cause
        ));

        server.verify();
    }

    @Test
    @DisplayName("웹훅 URL이 없으면 전송하지 않고 설정 오류를 반환한다")
    void send_WithoutWebhookUrl_ThrowsConfigurationError() {
        DiscordWebhookClient client = new DiscordWebhookClient(
                RestClient.create(),
                new DiscordAlertProperties()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("webhook URL");
    }

    private ServerErrorEvent event() {
        return new ServerErrorEvent(
                Instant.now(), "test", 500, "GET", "/boom",
                "RuntimeException", "boom", "trace-2", new RuntimeException("boom")
        );
    }
}
