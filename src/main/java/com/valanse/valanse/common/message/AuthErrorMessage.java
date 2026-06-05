package com.valanse.valanse.common.message;

public enum AuthErrorMessage {
    DELETE_PERMISSION_DENIED("삭제 권한 없음"),
    KAKAO_ACCESS_TOKEN_ISSUE_FAILED("AccessToken 발급 실패"),
    KAKAO_PROFILE_FETCH_FAILED("카카오 사용자 정보 조회 실패"),
    INVALID_REFRESH_TOKEN("유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND("저장된 리프레시 토큰이 없습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_MISMATCH("리프레시 토큰이 일치하지 않습니다."),
    EXPIRED_TOKEN("EXPIRED_TOKEN", "토큰이 만료되었습니다."),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_ERROR("TOKEN_ERROR", "토큰 처리 중 오류가 발생했습니다.");

    private final String errorCode;
    private final String message;

    AuthErrorMessage(String message) {
        this.errorCode = null;
        this.message = message;
    }

    AuthErrorMessage(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String message() {
        return message;
    }

    public String errorCode() {
        return errorCode;
    }

    public String tokenErrorResponse() {
        return "{\"error\":\"" + errorCode + "\",\"message\":\"" + message + "\"}";
    }
}
