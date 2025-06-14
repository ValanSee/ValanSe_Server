package com.valanse.valanse.service.AuthService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

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

    public Map<String, String> reissueAccessToken(String requestRefreshToken) {
        // 1. refresh token 유효성 검증
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new ApiException("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }

        // 2. subject (userId) 추출
        String userId = jwtTokenProvider.getSubject(requestRefreshToken);

        // 3. Redis에 저장된 토큰 조회
        String savedRefreshToken = refreshTokenService.getRefreshToken(userId);
        if (savedRefreshToken == null) {
            throw new ApiException("저장된 리프레시 토큰이 없습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED);
        }

        if (!savedRefreshToken.equals(requestRefreshToken)) {
            throw new ApiException("리프레시 토큰이 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        // 4. 새 access token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(Long.parseLong(userId), "USER");

        return Map.of("accessToken", newAccessToken);
    }
}
