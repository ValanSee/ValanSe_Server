package com.valanse.valanse.dto.Analytics;

/**
 * MauResponse API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public record MauResponse(
        String yearMonth,
        long anonymousMau,
        long loginMau,
        long withdrawnMau,
        long totalMau
) {
}
