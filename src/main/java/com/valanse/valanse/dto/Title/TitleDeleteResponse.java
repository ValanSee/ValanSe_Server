package com.valanse.valanse.dto.Title;

/**
 * TitleDeleteResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record TitleDeleteResponse(
        Long deletedTitleId,
        String deletedTitle,
        Long fallbackTitleId,
        String fallbackTitle,
        int reassignedCount,
        boolean active
) {
}
