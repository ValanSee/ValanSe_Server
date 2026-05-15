package com.valanse.valanse.dto.Title;

import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;

public record TitleAdminResponse(
        Long titleId,
        String code,
        String titleName,
        String description,
        Long price,
        TitleTier tier,
        TitleAcquisitionType acquisitionType,
        String requirementText,
        Integer displayOrder
) {
}
