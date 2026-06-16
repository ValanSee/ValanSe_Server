package com.valanse.valanse.dto.Title;

import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;

/**
 * TitleUpdateRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public record TitleUpdateRequest(
        String code,
        String title,
        String description,
        Long price,
        TitleTier tier,
        TitleAcquisitionType acquisitionType,
        String requirementText,
        Boolean active,
        Integer displayOrder
) {
}
