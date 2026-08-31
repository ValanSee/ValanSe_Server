package com.valanse.valanse.common.alert;

import com.valanse.valanse.service.ContentSeedService.ContentSeedBatchOutcome;
import com.valanse.valanse.service.ContentSeedService.ContentSeedItemFailure;
import com.valanse.valanse.service.ContentSeedService.ContentSeedRunResult;
import com.valanse.valanse.service.ContentSeedService.ContentSeedUsageSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
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

    @Test
    @DisplayName("부분 완료된 콘텐츠 시드 실행 결과를 게시글 링크·거절 사유와 함께 전송한다")
    void send_ContentSeedPartialCompletion_PostsSummaryEmbed() {
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
                          "username": "ValanSe Content Seed",
                          "allowed_mentions": {"parse": []},
                          "embeds": [{
                            "title": "콘텐츠 시드 실행 부분 완료",
                            "color": 15844367,
                            "fields": [
                              {"name": "트리거", "value": "SCHEDULED", "inline": true},
                              {"name": "환경", "value": "prod", "inline": true},
                              {"name": "게시글", "value": "2/3", "inline": true},
                              {"name": "상호작용", "value": "5/5", "inline": true},
                              {"name": "API 호출", "value": "2회", "inline": true},
                              {"name": "토큰 사용량", "value": "입력 1000 · 출력 500", "inline": true},
                              {"name": "예상 비용", "value": "$0.007000", "inline": true},
                              {"name": "소요 시간", "value": "65.4초", "inline": true},
                              {"name": "생성된 게시글", "value": "https://valanse.kr/votes/10\\nhttps://valanse.kr/votes/20", "inline": false},
                              {"name": "품질 거절 사유", "value": "제목 길이 위반 (1건)", "inline": false}
                            ]
                          }]
                        }
                        """, false))
                .andRespond(withNoContent());

        ContentSeedRunResult result = new ContentSeedRunResult(
                List.of(new ContentSeedBatchOutcome(1L, 3, 2, List.of(
                        new ContentSeedItemFailure("d1", "제목 길이 위반")))),
                List.of(new ContentSeedBatchOutcome(1L, 5, 5, List.of())),
                new ContentSeedUsageSummary(2, 1000, 500, new BigDecimal("0.007000")),
                List.of(10L, 20L)
        );
        ContentSeedRunEvent event = ContentSeedRunEvent.completed(
                "prod", "SCHEDULED", Instant.parse("2026-07-22T06:00:00Z"), 65_432L, result, "https://valanse.kr");

        client.send(event);

        server.verify();
    }

    @Test
    @DisplayName("콘텐츠 시드 실행이 치명적 오류로 중단되면 오류 요약을 전송한다")
    void send_ContentSeedFatalError_PostsErrorEmbed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        DiscordAlertProperties properties = new DiscordAlertProperties();
        properties.setWebhookUrl("https://discord.test/api/webhooks/1/token");
        DiscordWebhookClient client = new DiscordWebhookClient(builder.build(), properties);

        server.expect(once(), requestTo("https://discord.test/api/webhooks/1/token?wait=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "username": "ValanSe Content Seed",
                          "allowed_mentions": {"parse": []},
                          "embeds": [{
                            "title": "콘텐츠 시드 실행 실패",
                            "color": 15158332,
                            "fields": [
                              {"name": "트리거", "value": "ADMIN", "inline": true},
                              {"name": "환경", "value": "prod", "inline": true},
                              {"name": "오류", "value": "IllegalStateException: API Key 없음", "inline": false},
                              {"name": "소요 시간", "value": "0.5초", "inline": true}
                            ]
                          }]
                        }
                        """, false))
                .andRespond(withNoContent());

        ContentSeedRunEvent event = ContentSeedRunEvent.fatal(
                "prod", "ADMIN", Instant.parse("2026-07-22T06:00:00Z"), 500L,
                new IllegalStateException("API Key 없음"));

        client.send(event);

        server.verify();
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
