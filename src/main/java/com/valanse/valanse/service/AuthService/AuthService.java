package com.valanse.valanse.service.AuthService;

import java.util.Map;

/**
 * AuthService 기능의 비즈니스 계약을 정의하는 서비스 인터페이스 코드입니다.
 */
public interface AuthService {
    void logout();
    Map<String, String> reissueAccessToken(String requestRefreshToken);
}
