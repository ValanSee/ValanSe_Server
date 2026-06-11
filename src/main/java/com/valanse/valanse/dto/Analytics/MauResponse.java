package com.valanse.valanse.dto.Analytics;

public record MauResponse(
        String yearMonth,
        long anonymousMau,
        long loginMau,
        long withdrawnMau,
        long totalMau
) {
}
