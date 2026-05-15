package com.valanse.valanse.dto.Title;

public record TitlePurchaseResponse(
        Long titleId,
        String title,
        boolean owned,
        long remainingPoint
) {
}
