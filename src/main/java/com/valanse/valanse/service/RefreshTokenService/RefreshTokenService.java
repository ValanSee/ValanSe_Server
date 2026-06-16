package com.valanse.valanse.service.RefreshTokenService;

/**
 * RefreshTokenService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface RefreshTokenService {

    void saveRefreshToken(String userId, String refreshToken, long expirationMillis);
    String getRefreshToken(String userId);
    void deleteRefreshToken(String userId);

}
