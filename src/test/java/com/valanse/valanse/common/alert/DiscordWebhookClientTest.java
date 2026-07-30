package com.valanse.valanse.common.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

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

        assertThatThrownBy(() -> client.send(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("webhook URL");
    }

    @Test
    @DisplayName("Discord 5xx 응답은 지수 백오프로 재시도한다")
    void send_ServerErrors_RetriesWithExponentialBackoff() {
        TestClient testClient = testClient();

        testClient.server().expect(once(), requestTo(webhookUri())).andRespond(withServerError());
        testClient.server().expect(once(), requestTo(webhookUri()))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        testClient.server().expect(once(), requestTo(webhookUri())).andRespond(withNoContent());

        testClient.client().send(event());

        assertThat(testClient.delays()).containsExactly(
                Duration.ofMillis(500),
                Duration.ofSeconds(1)
        );
        testClient.server().verify();
    }

    @Test
    @DisplayName("Discord 429 응답은 Retry-After 헤더를 따라 재시도한다")
    void send_TooManyRequests_UsesRetryAfterHeader() {
        TestClient testClient = testClient();

        testClient.server().expect(once(), requestTo(webhookUri()))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header(HttpHeaders.RETRY_AFTER, "1.25"));
        testClient.server().expect(once(), requestTo(webhookUri())).andRespond(withNoContent());

        testClient.client().send(event());

        assertThat(testClient.delays()).containsExactly(Duration.ofMillis(1_250));
        testClient.server().verify();
    }

    @Test
    @DisplayName("재시도 대상이 아닌 4xx 응답은 즉시 실패한다")
    void send_NonRetryableClientError_DoesNotRetry() {
        TestClient testClient = testClient();
        testClient.server().expect(once(), requestTo(webhookUri()))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> testClient.client().send(event()))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        assertThat(testClient.delays()).isEmpty();
        testClient.server().verify();
    }

    @Test
    @DisplayName("연결 또는 응답 타임아웃은 재시도한다")
    void send_NetworkTimeout_Retries() {
        TestClient testClient = testClient();

        testClient.server().expect(once(), requestTo(webhookUri()))
                .andRespond(withException(new SocketTimeoutException("timeout")));
        testClient.server().expect(once(), requestTo(webhookUri())).andRespond(withNoContent());

        testClient.client().send(event());

        assertThat(testClient.delays()).containsExactly(Duration.ofMillis(500));
        testClient.server().verify();
    }

    private TestClient testClient() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DiscordAlertProperties properties = new DiscordAlertProperties();
        properties.setWebhookUrl("https://discord.test/api/webhooks/1/token");
        List<Duration> delays = new ArrayList<>();
        DiscordWebhookClient client = new DiscordWebhookClient(builder.build(), properties, delays::add);
        return new TestClient(client, server, delays);
    }

    private String webhookUri() {
        return "https://discord.test/api/webhooks/1/token?wait=true";
    }

    private ServerErrorEvent event() {
        return new ServerErrorEvent(
                Instant.now(), "test", 500, "GET", "/boom",
                "RuntimeException", "boom", "trace-2", new RuntimeException("boom")
        );
    }

    private record TestClient(
            DiscordWebhookClient client,
            MockRestServiceServer server,
            List<Duration> delays
    ) {
    }
}
