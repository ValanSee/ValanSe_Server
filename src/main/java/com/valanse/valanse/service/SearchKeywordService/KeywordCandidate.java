package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.domain.enums.SearchKeywordSource;

public record KeywordCandidate(Long id, SearchKeywordSource source, String keyword,
                               Integer monthlySearchVolume, Integer impressions) {
}
