package com.valanse.valanse.dto.Title;

import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;

/**
 * TitleUpdateResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record TitleUpdateResponse(
        Long titleId,
        String code,
        String title,
        String description,
        Long price,
        TitleTier tier,
        TitleAcquisitionType acquisitionType,
        String requirementText,
        boolean active,
        Integer displayOrder
) {
}
