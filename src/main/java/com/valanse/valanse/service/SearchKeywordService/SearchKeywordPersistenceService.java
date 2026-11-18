package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.domain.SearchKeyword;
import com.valanse.valanse.domain.SearchKeywordSnapshot;
import com.valanse.valanse.domain.enums.SearchKeywordSource;
import com.valanse.valanse.repository.SearchKeywordRepository;
import com.valanse.valanse.repository.SearchKeywordSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 출처 1개 분량을 독립 트랜잭션(REQUIRES_NEW)으로 저장한다. 한 출처의 DB 실패가
// 다른 출처 저장을 롤백하지 않도록 수집기와 별도 빈으로 분리했다(self-invocation 방지).
@Service
@RequiredArgsConstructor
public class SearchKeywordPersistenceService {

    private static final int MAX_KEYWORD_LENGTH = 100;

    private final SearchKeywordRepository keywordRepository;
    private final SearchKeywordSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveSource(SearchKeywordSource source, List<CollectedKeyword> collected) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);

        // 정규화 결과가 같은 원문은 첫 번째만 사용한다.
        Map<String, CollectedKeyword> deduped = new LinkedHashMap<>();
        for (CollectedKeyword item : collected) {
            String normalized = KeywordNormalizer.normalize(item.keyword());
            if (normalized.isEmpty() || normalized.length() > MAX_KEYWORD_LENGTH
                    || item.keyword().length() > MAX_KEYWORD_LENGTH) {
                continue;
            }
            deduped.putIfAbsent(normalized, item);
        }

        for (Map.Entry<String, CollectedKeyword> entry : deduped.entrySet()) {
            String normalized = entry.getKey();
            CollectedKeyword item = entry.getValue();

            SearchKeyword keyword = keywordRepository.findBySourceAndNormalizedKeyword(source, normalized)
                    .orElse(null);
            if (keyword == null) {
                keyword = keywordRepository.save(SearchKeyword.create(source, item.keyword(), normalized, now));
            }
            keyword.refreshMetrics(item.monthlySearchVolume(), item.impressions(), item.clicks(),
                    item.avgPosition(), now);

            SearchKeywordSnapshot snapshot = snapshotRepository
                    .findBySearchKeywordAndCollectedDate(keyword, today).orElse(null);
            if (snapshot == null) {
                snapshotRepository.save(SearchKeywordSnapshot.of(keyword, today, item.monthlySearchVolume(),
                        item.impressions(), item.clicks(), item.avgPosition()));
            } else {
                snapshot.overwrite(item.monthlySearchVolume(), item.impressions(), item.clicks(),
                        item.avgPosition());
            }
        }
        return deduped.size();
    }
}
