package com.valanse.valanse.service.AuthService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.common.message.AuthErrorMessage;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.service.RefreshTokenService.RefreshTokenServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
/**
 * JWT 로그아웃과 refresh token 기반 access token 재발급을 처리하는 서비스 코드입니다.
 * check: 재발급 시 role을 고정 USER로 넣지 말고 DB의 현재 권한을 조회하는 것이 안전합니다.
 */
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenServiceImpl refreshTokenService;

    /**
     * 현재 access token의 subject 기준으로 Redis refresh token을 삭제하는 로그아웃 메서드입니다.
     */
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

    /**
     * 요청 refresh token을 검증하고 새 access token을 발급하는 메서드입니다.
     * check: 새 access token의 role을 고정값이 아니라 현재 회원 권한에서 가져와야 합니다.
     */
    public Map<String, String> reissueAccessToken(String requestRefreshToken) {
        // 1. refresh token 유효성 검증
        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            throw new ApiException(AuthErrorMessage.INVALID_REFRESH_TOKEN.message(), HttpStatus.UNAUTHORIZED);
        }

        // 2. subject (userId) 추출
        String userId = jwtTokenProvider.getSubject(requestRefreshToken);

        // 3. Redis에 저장된 토큰 조회
        String savedRefreshToken = refreshTokenService.getRefreshToken(userId);
        if (savedRefreshToken == null) {
            throw new ApiException(AuthErrorMessage.REFRESH_TOKEN_NOT_FOUND.message(), HttpStatus.UNAUTHORIZED);
        }

        if (!savedRefreshToken.equals(requestRefreshToken)) {
            throw new ApiException(AuthErrorMessage.REFRESH_TOKEN_MISMATCH.message(), HttpStatus.UNAUTHORIZED);
        }

        // 4. 새 access token 발급
        String role = "0".equals(userId) ? Role.ADMIN.toString() : Role.USER.toString();
        String newAccessToken = jwtTokenProvider.createAccessToken(Long.parseLong(userId), role);

        return Map.of("accessToken", newAccessToken);
    }
}
