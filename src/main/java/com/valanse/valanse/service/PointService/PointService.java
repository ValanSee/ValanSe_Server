package com.valanse.valanse.service.PointService;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.PointType;

public interface PointService {
    void givePoint(Member member, PointType type, Long amount);
    void giveCommentPointIfAvailable(Member member);
}
