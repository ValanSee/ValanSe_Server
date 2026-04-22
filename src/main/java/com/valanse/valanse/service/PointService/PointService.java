package com.valanse.valanse.service.PointService;

import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.PointHistory.PointHistoryResponse;

public interface PointService {
    void givePoint(Long memberId, PointType type);
    PointHistoryResponse getPointHistory(Long memberId);
}
