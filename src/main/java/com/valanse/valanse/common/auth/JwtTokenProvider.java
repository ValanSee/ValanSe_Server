package com.valanse.valanse.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {
    // application.yml에서 주입받은 시크릿 키 (Base64 인코딩된 문자열)
    private final String secretKey;
    // 액세스 토큰 만료 시간 (분 단위)
    private final int accessTokenExpiration;   // 분 단위
    // 리프레시 토큰 만료 시간 (분 단위)
    private final int refreshTokenExpiration;
    // JWT 서명용 키 (HMAC-SHA256 알고리즘 사용)
    private Key SECRET_KEY;

    // 생성자에서 application.yml 설정값을 주입받고 서명 키 생성
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.access-token-expiration}") int accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}") int refreshTokenExpiration) {
        this.secretKey = secretKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.SECRET_KEY = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKey), SignatureAlgorithm.HS256.getJcaName());
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String role) {
//        Claims claims = Jwts.claims().setSubject(email);
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId)); // subject에 userId 저장
        claims.put("role", role); // role 클레임 추가 (권한 확인용)
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims) // 사용자 정보 넣기
                .setIssuedAt(now) // 발급 시간
                .setExpiration(new Date(now.getTime() + accessTokenExpiration * 60 * 1000L))
                .signWith(SECRET_KEY) // 시그니처
                .compact();  // 최종 JWT 문자열로 직렬화
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 유저 식별
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpiration * 60 * 1000L))
                .signWith(SECRET_KEY)
                .compact();
    }

    // 액세스 + 리프레시 토큰을 한 번에 발급
    public Map<String, String> createTokenPair(Long userId, String role) {
        String accessToken = createAccessToken(userId, role);
        String refreshToken = createRefreshToken(userId);  // 👈 수정된 버전 사용
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    // JWT에서 subject(userId)를 꺼내는 메서드
    public String getSubject(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // JWT가 유효한지 확인하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
