package com.valanse.valanse.service.SearchKeywordService;

import com.valanse.valanse.domain.enums.SearchKeywordSource;

import java.util.Map;

public record SearchKeywordCollectResult(
        Map<SearchKeywordSource, Integer> savedCounts,
        Map<SearchKeywordSource, String> failures
) {
}
