package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.common.config.SearchKeywordProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleSearchConsoleClientTest {

    private static final String URL =
            "https://www.googleapis.com/webmasters/v3/sites/https%3A%2F%2Fvalanse.kr%2F/searchAnalytics/query";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SearchKeywordProperties properties;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-15T16:00:00Z"), ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new SearchKeywordProperties();
        properties.getGoogle().setServiceAccountJsonBase64("e30=");
    }

    private GoogleSearchConsoleClient client() {
        return new GoogleSearchConsoleClient(builder.build(), properties, () -> "tok", clock);
    }

    @Test
    void encodes_site_url_once() {
        server.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client().fetch();

        server.verify();
    }

    @Test
    void sends_date_range_and_parses_rows() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.startDate").value("2026-10-17"))
                .andExpect(jsonPath("$.endDate").value("2026-11-13"))
                .andExpect(jsonPath("$.rowLimit").value(200))
                .andExpect(jsonPath("$.dimensions[0]").value("query"))
                .andRespond(withSuccess("""
                        {"rows":[
                          {"keys":["밸런스게임"],"clicks":3.0,"impressions":120.0,"position":4.256},
                          {"keys":["커플 밸런스게임"],"clicks":0.0,"impressions":15.0,"position":12.0}
                        ]}""", MediaType.APPLICATION_JSON));

        List<CollectedKeyword> result = client().fetch();

        assertThat(result).containsExactly(
                new CollectedKeyword("밸런스게임", null, 120, 3, new BigDecimal("4.26")),
                new CollectedKeyword("커플 밸런스게임", null, 15, 0, new BigDecimal("12.00")));
    }

    @Test
    void empty_rows_returns_empty() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client().fetch()).isEmpty();
    }

    @Test
    void isConfigured_follows_properties() {
        assertThat(client().isConfigured()).isTrue();
        properties.getGoogle().setServiceAccountJsonBase64("");
        assertThat(client().isConfigured()).isFalse();
    }
}
