package com.valanse.valanse.service.SearchKeywordService;

import com.fasterxml.jackson.databind.JsonNode;
import com.valanse.valanse.common.config.SearchKeywordProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GoogleSearchConsoleClient {

    private static final String BASE_URL = "https://www.googleapis.com/webmasters/v3/sites/";
    private static final String QUERY_PATH = "/searchAnalytics/query";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final RestClient restClient;
    private final SearchKeywordProperties properties;
    private final GoogleAccessTokenProvider tokenProvider;
    private final Clock clock;

    public GoogleSearchConsoleClient(RestClient.Builder builder, SearchKeywordProperties properties,
                                     GoogleAccessTokenProvider tokenProvider, Clock clock) {
        this.restClient = builder.build();
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.clock = clock;
    }

    public boolean isConfigured() {
        return properties.getGoogle().isConfigured();
    }

    public List<CollectedKeyword> fetch() {
        SearchKeywordProperties.Google google = properties.getGoogle();
        LocalDate today = LocalDate.now(clock.withZone(SEOUL));
        Map<String, Object> body = Map.of(
                "startDate", today.minusDays(30).toString(),
                "endDate", today.minusDays(3).toString(),
                "dimensions", List.of("query"),
                "rowLimit", google.getMaxKeywords());

        // Pass a pre-encoded URI so the site URL is not encoded a second time.
        String encodedSite = URLEncoder.encode(google.getSiteUrl(), StandardCharsets.UTF_8).replace("+", "%20");
        JsonNode response = restClient.post()
                .uri(URI.create(BASE_URL + encodedSite + QUERY_PATH))
                .header("Authorization", "Bearer " + tokenProvider.accessToken())
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        List<CollectedKeyword> result = new ArrayList<>();
        if (response == null || !response.path("rows").isArray()) {
            return result;
        }
        for (JsonNode row : response.get("rows")) {
            String keyword = row.path("keys").path(0).asText("").trim();
            if (keyword.isEmpty()) {
                continue;
            }
            result.add(new CollectedKeyword(keyword, null,
                    (int) Math.round(row.path("impressions").asDouble()),
                    (int) Math.round(row.path("clicks").asDouble()),
                    BigDecimal.valueOf(row.path("position").asDouble()).setScale(2, RoundingMode.HALF_UP)));
        }
        return result;
    }
}
