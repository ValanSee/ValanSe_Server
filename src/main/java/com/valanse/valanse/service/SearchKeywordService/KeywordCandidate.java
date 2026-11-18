package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.domain.enums.SearchKeywordSource;

import java.util.List;

// ids는 이 후보로 합쳐진 모든 DB 행의 id다. 네이버·GSC 양쪽에 같은 정규화 키워드가 있으면
// 하나의 후보로 합쳐지고, 사용 시 ids 전체에 사용 기록이 남아야 56일 재사용 제한을 우회하지 않는다.
public record KeywordCandidate(List<Long> ids, SearchKeywordSource source, String keyword,
                               Integer monthlySearchVolume, Integer impressions) {
}
