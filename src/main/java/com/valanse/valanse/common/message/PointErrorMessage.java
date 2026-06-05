package com.valanse.valanse.common.message;

public enum PointErrorMessage {
    INVALID_REWARD_TYPE("포인트 지급 타입이 아닙니다."),
    USAGE_POINT_INVALID("사용 포인트는 0보다 커야 합니다.");

    private final String message;

    PointErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
