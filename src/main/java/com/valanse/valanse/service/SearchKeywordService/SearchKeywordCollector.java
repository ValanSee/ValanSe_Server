package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.domain.enums.SearchKeywordSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

// 네이버 검색광고·Google Search Console 키워드를 수집해 DB에 반영한다.
// 키가 없는 출처는 HTTP 호출 없이 건너뛰고, 출처별 실패는 서로 격리한다.
@Service
@RequiredArgsConstructor
public class SearchKeywordCollector {

    private static final Logger log = LoggerFactory.getLogger(SearchKeywordCollector.class);

    private final NaverSearchAdKeywordClient naverClient;
    private final GoogleSearchConsoleClient googleClient;
    private final SearchKeywordPersistenceService persistenceService;

    public SearchKeywordCollectResult collect() {
        Map<SearchKeywordSource, Integer> saved = new EnumMap<>(SearchKeywordSource.class);
        Map<SearchKeywordSource, String> failures = new EnumMap<>(SearchKeywordSource.class);

        collectSource(SearchKeywordSource.NAVER_SEARCH_AD, naverClient::isConfigured, naverClient::fetch,
                saved, failures);
        collectSource(SearchKeywordSource.GOOGLE_SEARCH_CONSOLE, googleClient::isConfigured, googleClient::fetch,
                saved, failures);
        return new SearchKeywordCollectResult(saved, failures);
    }

    private void collectSource(SearchKeywordSource source, BooleanSupplier configured,
                               Supplier<List<CollectedKeyword>> fetcher,
                               Map<SearchKeywordSource, Integer> saved,
                               Map<SearchKeywordSource, String> failures) {
        saved.put(source, 0);
        if (!configured.getAsBoolean()) {
            return;
        }
        try {
            int count = persistenceService.saveSource(source, fetcher.get());
            saved.put(source, count);
            log.info("SEARCH_KEYWORD_COLLECTED source={} saved={}", source, count);
        } catch (Exception e) {
            failures.put(source, String.valueOf(e.getMessage()));
            log.warn("SEARCH_KEYWORD_COLLECT_FAILED source={}", source, e);
        }
    }
}
