package com.valanse.valanse.dto.PointHistory;

import com.valanse.valanse.domain.enums.PointType;

import java.util.List;

/**
 * PointHistoryResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record PointHistoryResponse(
        List<PointHistoryItem> pointHistory
) {
    /**
     * API 요청과 응답 데이터를 전달하기 위한 DTO 코드입니다.
     */
    public record PointHistoryItem(
            Long id,
            Long amount,
            Long remainingPoint,
            PointType type,
            String typeDescription,
            String createdAt
    ) {}
}
