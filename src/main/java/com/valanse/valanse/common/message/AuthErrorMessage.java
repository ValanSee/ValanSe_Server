package com.valanse.valanse.common.message;

public enum AuthErrorMessage {
    DELETE_PERMISSION_DENIED("삭제 권한 없음");

    private final String message;

    AuthErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
