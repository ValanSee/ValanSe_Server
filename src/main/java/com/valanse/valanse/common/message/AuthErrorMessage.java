package com.valanse.valanse.common.message;

/**
 * AuthErrorMessage 도메인에서 사용하는 고정 선택 값을 정의하는 enum 코드입니다.
 */
public enum AuthErrorMessage {
    DELETE_PERMISSION_DENIED("삭제 권한 없음"),
    ADMIN_ONLY("관리자만 접근 가능합니다."),
    KAKAO_ACCESS_TOKEN_ISSUE_FAILED("AccessToken 발급 실패"),
    KAKAO_REDIRECT_URI_NOT_ALLOWED("허용되지 않은 카카오 redirect URI입니다."),
    KAKAO_PROFILE_FETCH_FAILED("카카오 사용자 정보 조회 실패"),
    KAKAO_REFRESH_TOKEN_NOT_FOUND("카카오 RefreshToken이 존재하지 않습니다."),
    KAKAO_ACCESS_TOKEN_REISSUE_FAILED("카카오 access token 재발급 실패"),
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

    /**
     * AuthErrorMessage의 message 기능을 수행하는 메서드입니다.
     */
    public String message() {
        return message;
    }

    /**
     * AuthErrorMessage의 errorCode 기능을 수행하는 메서드입니다.
     */
    public String errorCode() {
        return errorCode;
    }

    /**
     * tokenErrorResponse 형태로 데이터를 변환하는 메서드입니다.
     */
    public String tokenErrorResponse() {
        return "{\"error\":\"" + errorCode + "\",\"message\":\"" + message + "\"}";
    }
}
