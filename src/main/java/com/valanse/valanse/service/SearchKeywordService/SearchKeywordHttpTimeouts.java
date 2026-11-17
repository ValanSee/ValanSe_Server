package com.valanse.valanse.service.SearchKeywordService;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

// 검색 키워드 외부 API(네이버 검색광고·Google Search Console) 호출의 명시적 타임아웃.
// 응답이 멈춘 외부 API가 일 단위 수집 스케줄러 락(PT30M)을 오래 붙잡지 않게 한다.
// 구성 방식은 DiscordAlertConfiguration과 같다(JDK HttpClient + JdkClientHttpRequestFactory).
final class SearchKeywordHttpTimeouts {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

    private SearchKeywordHttpTimeouts() {
    }

    static JdkClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    static RestClient build(RestClient.Builder builder) {
        return builder.requestFactory(requestFactory()).build();
    }
}
