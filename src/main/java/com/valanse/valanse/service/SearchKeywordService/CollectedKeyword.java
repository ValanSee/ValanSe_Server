package com.valanse.valanse.service.SearchKeywordService;

import java.math.BigDecimal;

public record CollectedKeyword(
        String keyword,
        Integer monthlySearchVolume,
        Integer impressions,
        Integer clicks,
        BigDecimal avgPosition
) {
}
