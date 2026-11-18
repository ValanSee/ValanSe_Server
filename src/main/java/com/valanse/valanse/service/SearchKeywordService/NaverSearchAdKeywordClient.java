package com.valanse.valanse.service.SearchKeywordService;

import com.fasterxml.jackson.databind.JsonNode;
import com.valanse.valanse.common.config.SearchKeywordProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NaverSearchAdKeywordClient {

    private static final String BASE_URL = "https://api.searchad.naver.com";
    private static final String PATH = "/keywordstool";
    private static final int BATCH_SIZE = 5;
    private static final long BATCH_DELAY_MILLIS = 300L;

    private final RestClient restClient;
    private final SearchKeywordProperties properties;
    private final Clock clock;
    private final BatchPause pause;

    @Autowired
    public NaverSearchAdKeywordClient(RestClient.Builder builder, SearchKeywordProperties properties, Clock clock) {
        this(SearchKeywordHttpTimeouts.build(builder), properties, clock, () -> Thread.sleep(BATCH_DELAY_MILLIS));
    }

    // 테스트용: MockRestServiceServer에 묶인 RestClient를 그대로 받는다(타임아웃 팩토리로 덮어쓰지 않음).
    NaverSearchAdKeywordClient(RestClient restClient, SearchKeywordProperties properties,
                               Clock clock, BatchPause pause) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
        this.pause = pause;
    }

    public boolean isConfigured() {
        return properties.getNaver().isConfigured();
    }

    public List<CollectedKeyword> fetch() {
        SearchKeywordProperties.Naver naver = properties.getNaver();
        List<String> hints = naver.getHintKeywords().stream()
                .map(h -> h == null ? "" : h.replaceAll("\\s+", ""))
                .filter(h -> !h.isEmpty())
                .toList();

        Map<String, CollectedKeyword> merged = new LinkedHashMap<>();
        for (int i = 0; i < hints.size(); i += BATCH_SIZE) {
            if (i > 0) {
                pausePolitely();
            }
            String joined = String.join(",", hints.subList(i, Math.min(i + BATCH_SIZE, hints.size())));
            for (CollectedKeyword keyword : request(naver, joined)) {
                merged.merge(keyword.keyword(), keyword, NaverSearchAdKeywordClient::larger);
            }
        }

        return new ArrayList<>(merged.values()).stream()
                .sorted(Comparator.comparing(
                        (CollectedKeyword k) -> k.monthlySearchVolume() == null ? -1 : k.monthlySearchVolume())
                        .reversed())
                .limit(naver.getMaxKeywords())
                .toList();
    }

    static String sign(String secretKey, long timestamp, String method, String path) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((timestamp + "." + method + "." + path).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign Naver SearchAd request", e);
        }
    }

    private List<CollectedKeyword> request(SearchKeywordProperties.Naver naver, String hints) {
        long timestamp = clock.millis();
        JsonNode body = restClient.get()
                .uri(BASE_URL + PATH + "?hintKeywords={hints}&showDetail=1", hints)
                .header("X-Timestamp", String.valueOf(timestamp))
                .header("X-API-KEY", naver.getApiKey())
                .header("X-Customer", naver.getCustomerId())
                .header("X-Signature", sign(naver.getSecretKey(), timestamp, "GET", PATH))
                .retrieve()
                .body(JsonNode.class);

        List<CollectedKeyword> result = new ArrayList<>();
        if (body == null || !body.path("keywordList").isArray()) {
            return result;
        }
        for (JsonNode item : body.get("keywordList")) {
            String keyword = item.path("relKeyword").asText("").trim();
            if (keyword.isEmpty()) {
                continue;
            }
            result.add(new CollectedKeyword(keyword,
                    total(count(item.get("monthlyPcQcCnt")), count(item.get("monthlyMobileQcCnt"))),
                    null, null, null));
        }
        return result;
    }

    private static CollectedKeyword larger(CollectedKeyword a, CollectedKeyword b) {
        int va = a.monthlySearchVolume() == null ? -1 : a.monthlySearchVolume();
        int vb = b.monthlySearchVolume() == null ? -1 : b.monthlySearchVolume();
        return vb > va ? b : a;
    }

    private static Integer count(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        String text = node.asText("").trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.startsWith("<")) {
            return 5;
        }
        try {
            return Integer.parseInt(text.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer total(Integer a, Integer b) {
        if (a == null && b == null) {
            return null;
        }
        return (a == null ? 0 : a) + (b == null ? 0 : b);
    }

    private void pausePolitely() {
        try {
            pause.pause();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    interface BatchPause {
        void pause() throws InterruptedException;
    }
}
