package com.valanse.valanse.dto.Title;

import com.valanse.valanse.domain.enums.TitleAcquisitionType;
import com.valanse.valanse.domain.enums.TitleTier;

import java.util.List;

/**
 * TitleListResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record TitleListResponse(
        List<TitleSummaryResponse> defaultTitles,
        List<TitleSummaryResponse> ownedTitles,
        List<TitleSummaryResponse> lockedTitles
) {
    /**
     * TitleSummaryResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
     */
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
            String lockReason
    ) {
    }
}
