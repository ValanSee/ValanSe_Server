package com.valanse.valanse.service;

import com.valanse.valanse.common.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {


    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public void logout() {
        // SecurityContext에서 현재 인증 정보(Authentication) 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // JWT 토큰은 credentials에 저장되어 있음
        String token = (String) authentication.getCredentials();

        // JWT에서 userId(subject) 추출
        String userId = jwtTokenProvider.getSubject(token);

        // Redis에서 RefreshToken 삭제
        refreshTokenService.deleteRefreshToken(userId);
    }


}
