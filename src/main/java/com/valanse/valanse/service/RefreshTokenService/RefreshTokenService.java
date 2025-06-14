package com.valanse.valanse.service.RefreshTokenService;

public interface RefreshTokenService {

    void saveRefreshToken(String userId, String refreshToken, long expirationMillis);
    String getRefreshToken(String userId);
    void deleteRefreshToken(String userId);

}
