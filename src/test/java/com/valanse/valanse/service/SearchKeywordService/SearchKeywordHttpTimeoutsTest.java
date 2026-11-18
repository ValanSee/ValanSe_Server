package com.valanse.valanse.service.SearchKeywordService;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SearchKeywordHttpTimeoutsTest {

    @Test
    void request_factory_uses_connect_5s_and_read_20s() {
        JdkClientHttpRequestFactory factory = SearchKeywordHttpTimeouts.requestFactory();

        HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(factory, "httpClient");
        assertThat(httpClient.connectTimeout()).contains(Duration.ofSeconds(5));
        assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(Duration.ofSeconds(20));
    }
}
