package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.common.config.SearchKeywordProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverSearchAdKeywordClientTest {

    private static final String BASE = "https://api.searchad.naver.com/keywordstool";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SearchKeywordProperties properties;
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new SearchKeywordProperties();
        properties.getNaver().setApiKey("api-key");
        properties.getNaver().setSecretKey("secret");
        properties.getNaver().setCustomerId("cust-1");
    }

    private NaverSearchAdKeywordClient client() {
        return new NaverSearchAdKeywordClient(builder.build(), properties, clock, () -> { });
    }

    private static org.springframework.test.web.client.RequestMatcher hint(String expected) {
        return request -> assertThat(UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("hintKeywords"))
                .satisfies(raw -> assertThat(URLDecoder.decode(raw, StandardCharsets.UTF_8)).isEqualTo(expected));
    }

    private static String json(String body) {
        return body;
    }

    @Test
    void signs_request_headers() {
        properties.getNaver().setHintKeywords(List.of("밸런스 게임"));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(hint("밸런스게임"))
                .andExpect(queryParam("showDetail", "1"))
                .andExpect(header("X-Timestamp", "1700000000000"))
                .andExpect(header("X-API-KEY", "api-key"))
                .andExpect(header("X-Customer", "cust-1"))
                .andExpect(header("X-Signature",
                        NaverSearchAdKeywordClient.sign("secret", 1700000000000L, "GET", "/keywordstool")))
                .andRespond(withSuccess(json("{\"keywordList\":[]}"), MediaType.APPLICATION_JSON));

        client().fetch();

        server.verify();
    }

    @Test
    void parses_less_than_ten_and_missing_counts() {
        properties.getNaver().setHintKeywords(List.of("h"));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withSuccess(json("{\"keywordList\":["
                        + "{\"relKeyword\":\"A\",\"monthlyPcQcCnt\":\"< 10\",\"monthlyMobileQcCnt\":120},"
                        + "{\"relKeyword\":\"B\"}]}"), MediaType.APPLICATION_JSON));

        List<CollectedKeyword> result = client().fetch();

        assertThat(result).extracting(CollectedKeyword::keyword).containsExactly("A", "B");
        assertThat(result.get(0).monthlySearchVolume()).isEqualTo(125);
        assertThat(result.get(1).monthlySearchVolume()).isNull();
    }

    @Test
    void batches_hints_by_five() {
        properties.getNaver().setHintKeywords(List.of("a", "b", "c", "d", "e", "f"));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andExpect(hint("a,b,c,d,e"))
                .andRespond(withSuccess(json("{\"keywordList\":[]}"), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andExpect(hint("f"))
                .andRespond(withSuccess(json("{\"keywordList\":[]}"), MediaType.APPLICATION_JSON));

        client().fetch();

        server.verify();
    }

    @Test
    void dedupes_and_limits() {
        properties.getNaver().setHintKeywords(List.of("a", "b", "c", "d", "e", "f"));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withSuccess(json("{\"keywordList\":["
                        + "{\"relKeyword\":\"X\",\"monthlyPcQcCnt\":10,\"monthlyMobileQcCnt\":10},"
                        + "{\"relKeyword\":\"Y\",\"monthlyPcQcCnt\":5,\"monthlyMobileQcCnt\":5}]}"),
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withSuccess(json("{\"keywordList\":["
                        + "{\"relKeyword\":\"X\",\"monthlyPcQcCnt\":100,\"monthlyMobileQcCnt\":100}]}"),
                        MediaType.APPLICATION_JSON));

        List<CollectedKeyword> all = client().fetch();
        assertThat(all).extracting(CollectedKeyword::keyword).containsExactly("X", "Y");
        assertThat(all.get(0).monthlySearchVolume()).isEqualTo(200);
    }

    @Test
    void limits_to_max_keywords() {
        properties.getNaver().setHintKeywords(List.of("a"));
        properties.getNaver().setMaxKeywords(1);
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE)))
                .andRespond(withSuccess(json("{\"keywordList\":["
                        + "{\"relKeyword\":\"X\",\"monthlyPcQcCnt\":1,\"monthlyMobileQcCnt\":1},"
                        + "{\"relKeyword\":\"Y\",\"monthlyPcQcCnt\":50,\"monthlyMobileQcCnt\":5}]}"),
                        MediaType.APPLICATION_JSON));

        assertThat(client().fetch()).extracting(CollectedKeyword::keyword).containsExactly("Y");
    }

    @Test
    void sign_matches_hmac_sha256_base64() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(
                mac.doFinal("1.GET./keywordstool".getBytes(StandardCharsets.UTF_8)));

        assertThat(NaverSearchAdKeywordClient.sign("secret", 1L, "GET", "/keywordstool")).isEqualTo(expected);
    }

    @Test
    void is_configured_follows_properties() {
        assertThat(client().isConfigured()).isTrue();
        properties.getNaver().setSecretKey("");
        assertThat(client().isConfigured()).isFalse();
    }
}
