package com.valanse.valanse.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
/**
 * Redis에 refresh token을 저장, 조회, 삭제하는 인증 보조 서비스 코드입니다.
 */
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * refresh token을 만료 시간과 함께 Redis에 저장하는 메서드입니다.
     */
    public void saveRefreshToken(String userId, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set("RT:" + userId, refreshToken, expirationMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 사용자 식별자 기준으로 Redis refresh token을 조회하는 메서드입니다.
     */
    public String getRefreshToken(String userId) {
        return redisTemplate.opsForValue().get("RT:" + userId);
    }

    /**
     * 사용자 식별자 기준으로 Redis refresh token을 삭제하는 메서드입니다.
     */
    public void deleteRefreshToken(String userId) {
        redisTemplate.delete("RT:" + userId);
    }
}

