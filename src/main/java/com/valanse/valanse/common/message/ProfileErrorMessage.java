package com.valanse.valanse.common.message;

/**
 * ProfileErrorMessage 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum ProfileErrorMessage {
    PROFILE_NOT_FOUND("회원 프로필이 존재하지 않습니다.");

    private final String message;

    ProfileErrorMessage(String message) {
        this.message = message;
    }

    /**
     * ProfileErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }
}
