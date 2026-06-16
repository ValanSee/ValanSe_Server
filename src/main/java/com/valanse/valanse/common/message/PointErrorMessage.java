package com.valanse.valanse.common.message;

/**
 * PointErrorMessage 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum PointErrorMessage {
    INVALID_REWARD_TYPE("포인트 지급 타입이 아닙니다."),
    USAGE_POINT_INVALID("사용 포인트는 0보다 커야 합니다.");

    private final String message;

    PointErrorMessage(String message) {
        this.message = message;
    }

    /**
     * PointErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }
}
