package com.valanse.valanse.common.message;

public enum ProfileErrorMessage {
    PROFILE_NOT_FOUND("회원 프로필이 존재하지 않습니다.");

    private final String message;

    ProfileErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
