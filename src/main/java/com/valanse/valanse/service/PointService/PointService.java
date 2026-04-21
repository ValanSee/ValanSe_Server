package com.valanse.valanse.service.PointService;

import com.valanse.valanse.domain.enums.PointType;

public interface PointService {
    void givePoint(Long memberId, PointType type);
}
