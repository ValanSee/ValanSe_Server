package com.valanse.valanse.service.AppleService;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Login.AppleIdentity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleServiceImplTest {

    private static final String BUNDLE_ID = "com.valanse";
    private static final String SERVICES_ID = "com.valanse.web";

    private AppleServiceImpl appleService;
    private KeyPair rsaKeyPair;
    private KeyPair ecKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(2048);
        rsaKeyPair = rsa.generateKeyPair();

        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(new ECGenParameterSpec("secp256r1"));
        ecKeyPair = ec.generateKeyPair();

        String pem = "-----BEGIN PRIVATE KEY-----\\n"
                + Base64.getEncoder().encodeToString(ecKeyPair.getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----";

        appleService = new AppleServiceImpl();
        ReflectionTestUtils.setField(appleService, "teamId", "TEAM123456");
        ReflectionTestUtils.setField(appleService, "keyId", "KEY1234567");
        ReflectionTestUtils.setField(appleService, "privateKey", pem);
        ReflectionTestUtils.setField(appleService, "clientIds", BUNDLE_ID + "," + SERVICES_ID);
        ReflectionTestUtils.setField(appleService, "allowedRedirectUris", "https://valanse.kr/oauth/apple/redirect");

        // Apple JWKS 대신 테스트 RSA 공개키로 검증 (검증 규칙은 운영과 동일)
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) rsaKeyPair.getPublic()).build();
        decoder.setJwtValidator(AppleServiceImpl.tokenValidator(Set.of(BUNDLE_ID, SERVICES_ID)));
        ReflectionTestUtils.setField(appleService, "jwtDecoder", decoder);
    }

    private String identityToken(String issuer, String audience, Instant expiresAt) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("001234.abcdef.0123")
                .claim("email", "abc@privaterelay.appleid.com")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(rsaKeyPair.getPrivate()));
        return jwt.serialize();
    }

    @Test
    @DisplayName("앱 번들 ID 로 발급된 identity token 을 검증하고 사용자 정보를 꺼낸다")
    void verifyIdentityToken_ValidAppToken_ReturnsIdentity() throws Exception {
        String token = identityToken(AppleServiceImpl.APPLE_ISSUER, BUNDLE_ID, Instant.now().plusSeconds(600));

        AppleIdentity identity = appleService.verifyIdentityToken(token);

        assertThat(identity.userId()).isEqualTo("001234.abcdef.0123");
        assertThat(identity.email()).isEqualTo("abc@privaterelay.appleid.com");
        assertThat(identity.clientId()).isEqualTo(BUNDLE_ID);
    }

    @Test
    @DisplayName("웹 Services ID 로 발급된 identity token 은 clientId 가 Services ID 다")
    void verifyIdentityToken_ValidWebToken_ReturnsServicesId() throws Exception {
        String token = identityToken(AppleServiceImpl.APPLE_ISSUER, SERVICES_ID, Instant.now().plusSeconds(600));

        assertThat(appleService.verifyIdentityToken(token).clientId()).isEqualTo(SERVICES_ID);
    }

    @Test
    @DisplayName("다른 앱용으로 발급된 token(aud 불일치)은 401 로 거부한다")
    void verifyIdentityToken_OtherAudience_ThrowsUnauthorized() throws Exception {
        String token = identityToken(AppleServiceImpl.APPLE_ISSUER, "com.other.app", Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> appleService.verifyIdentityToken(token))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Apple 이 아닌 발급자의 token 은 401 로 거부한다")
    void verifyIdentityToken_WrongIssuer_ThrowsUnauthorized() throws Exception {
        String token = identityToken("https://evil.example", BUNDLE_ID, Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> appleService.verifyIdentityToken(token))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("만료된 token 은 401 로 거부한다")
    void verifyIdentityToken_Expired_ThrowsUnauthorized() throws Exception {
        String token = identityToken(AppleServiceImpl.APPLE_ISSUER, BUNDLE_ID, Instant.now().minusSeconds(3600));

        assertThatThrownBy(() -> appleService.verifyIdentityToken(token))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("client secret 은 .p8 키로 서명한 ES256 JWT 이고 Apple 이 요구하는 클레임을 담는다")
    void createClientSecret_ReturnsSignedJwt() {
        String secret = appleService.createClientSecret(BUNDLE_ID);

        Jws<Claims> parsed = Jwts.parserBuilder()
                .setSigningKey(ecKeyPair.getPublic())
                .build()
                .parseClaimsJws(secret);

        assertThat(parsed.getHeader().getKeyId()).isEqualTo("KEY1234567");
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(parsed.getBody().getIssuer()).isEqualTo("TEAM123456");
        assertThat(parsed.getBody().getSubject()).isEqualTo(BUNDLE_ID);
        assertThat(parsed.getBody().getAudience()).isEqualTo(AppleServiceImpl.APPLE_ISSUER);
    }

    @Test
    @DisplayName("화이트리스트에 없는 redirect URI 는 Apple 요청 전에 거부한다")
    void issueRefreshToken_NotAllowedRedirectUri_ThrowsBadRequest() {
        assertThatThrownBy(() -> appleService.issueRefreshToken("code", SERVICES_ID, "https://evil.example/callback"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Apple 설정이 비어 있으면 503 을 반환한다")
    void verifyIdentityToken_NotConfigured_ThrowsServiceUnavailable() {
        ReflectionTestUtils.setField(appleService, "teamId", "");

        assertThatThrownBy(() -> appleService.verifyIdentityToken("token"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Apple refresh token 이 없는 회원은 revoke 요청 없이 400 을 반환한다")
    void revoke_MissingRefreshToken_ThrowsBadRequest() {
        Member member = Member.builder().appleClientId(BUNDLE_ID).build();

        assertThatThrownBy(() -> appleService.revoke(member))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
