package com.valanse.valanse.dto.Login;

/**
 * ReissueRequestDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class ReissueRequestDto {

    private String refreshToken;

    /**
     * 사용자 식별자 기준으로 Redis refresh token을 조회하는 메서드입니다.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * ReissueRequestDto의 setRefreshToken 기능을 수행하는 메서드입니다.
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}