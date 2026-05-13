package com.valanse.valanse.dto.Title;

import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;

import java.util.List;

public record TitleListResponse(
        List<TitleSummaryResponse> defaultTitles,
        List<TitleSummaryResponse> ownedTitles,
        List<TitleSummaryResponse> lockedTitles
) {
    public record TitleSummaryResponse(
            Long titleId,
            String title,
            String description,
            TitleTier tier,
            TitleAcquisitionType acquisitionType,
            boolean owned,
            boolean equipped,
            boolean locked,
            Long price,
            String requirementText,
            String lockReason,
            String colorHex
    ) {
    }
}
