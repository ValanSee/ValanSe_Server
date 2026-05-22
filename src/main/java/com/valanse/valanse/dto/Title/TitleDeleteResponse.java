package com.valanse.valanse.dto.Title;

public record TitleDeleteResponse(
        Long deletedTitleId,
        String deletedTitle,
        Long fallbackTitleId,
        String fallbackTitle,
        int reassignedCount,
        boolean active
) {
}
