package com.valanse.valanse.dto.PointHistory;

import com.valanse.valanse.domain.enums.PointType;

import java.util.List;

public record PointHistoryResponse(
        List<PointHistoryItem> pointHistory
) {
    public record PointHistoryItem(
            Long id,
            Long amount,
            PointType type,
            String typeDescription,
            String createdAt
    ) {}
}
