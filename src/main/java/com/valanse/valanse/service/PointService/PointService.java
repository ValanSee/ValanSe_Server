package com.valanse.valanse.service.PointService;

import com.valanse.valanse.domain.enums.PointType;
import com.valanse.valanse.dto.PointHistory.PointHistoryResponse;

/**
 * PointService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface PointService {
    void givePoint(Long memberId, PointType type);
    void recordPointUsage(Long memberId, long amount, PointType type);
    PointHistoryResponse getPointHistory(Long memberId);
}
