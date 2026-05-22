package com.valanse.valanse.dto.Title;

import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;

public record TitleCreateResponse(
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
