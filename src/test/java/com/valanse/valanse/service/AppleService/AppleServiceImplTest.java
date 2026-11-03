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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleServiceImplTest {

    private static final String BUNDLE_ID = "com.valanse";
    private static final String SERVICES_ID = "com.valanse.web";
    private static final String APPLE_USER_ID = "001234.abcdef.0123";
    private static final String TOKEN_URI = AppleServiceImpl.APPLE_ISSUER + "/auth/token";
    private static final String REVOKE_URI = AppleServiceImpl.APPLE_ISSUER + "/auth/revoke";

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

    private String identityToken(String issuer, String audience, String subject, Instant expiresAt) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(subject)
                .claim("email", "abc@privaterelay.appleid.com")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(rsaKeyPair.getPrivate()));
        return jwt.serialize();
    }

    private String identityToken(String issuer, String audience, Instant expiresAt) throws JOSEException {
        return identityToken(issuer, audience, APPLE_USER_ID, expiresAt);
    }

    /**
     * RestClient 호출을 MockRestServiceServer 로 가로채도록 appleService.restClient 필드를 교체한다.
     */
    private MockRestServiceServer bindMockServer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(appleService, "restClient", builder.build());
        return server;
    }

    private String tokenResponseJson(String refreshToken, String idToken) {
        StringBuilder json = new StringBuilder("{\"access_token\":\"apple-access\"");
        if (refreshToken != null) {
            json.append(",\"refresh_token\":\"").append(refreshToken).append('"');
        }
        if (idToken != null) {
            json.append(",\"id_token\":\"").append(idToken).append('"');
        }
        json.append('}');
        return json.toString();
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
        AppleIdentity expectedIdentity = new AppleIdentity(APPLE_USER_ID, null, SERVICES_ID);

        assertThatThrownBy(() -> appleService.issueRefreshToken("code", expectedIdentity, "https://evil.example/callback"))
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
    @DisplayName("Apple refresh token 이 없는 회원은 Apple 쪽에 아무 요청도 보내지 않고 조용히 통과한다 (탈퇴를 막지 않기 위함)")
    void revoke_MissingRefreshToken_SkipsApiCallWithoutError() {
        Member member = Member.builder().appleClientId(BUNDLE_ID).build();

        // revoke 대상 URI 에 expectation 을 걸지 않는다 — 구현이 실수로 호출을 시도하면
        // MockRestServiceServer 가 "예상하지 못한 요청"으로 예외를 던져 테스트가 실패한다.
        bindMockServer();

        assertThatCode(() -> appleService.revoke(member)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("교환 응답의 id_token 이 최초 identity 와 같은 사용자면 refresh token 을 반환한다")
    void issueRefreshToken_MatchingExchangeIdentity_ReturnsRefreshToken() throws Exception {
        AppleIdentity expectedIdentity = new AppleIdentity(APPLE_USER_ID, "user@privaterelay.appleid.com", BUNDLE_ID);
        String exchangeIdToken = identityToken(AppleServiceImpl.APPLE_ISSUER, BUNDLE_ID, Instant.now().plusSeconds(600));

        MockRestServiceServer server = bindMockServer();
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponseJson("apple-refresh-token", exchangeIdToken), MediaType.APPLICATION_JSON));

        String refreshToken = appleService.issueRefreshToken("auth-code", expectedIdentity, null);

        assertThat(refreshToken).isEqualTo("apple-refresh-token");
        server.verify();
    }

    @Test
    @DisplayName("교환 응답에 id_token 이 없으면 거부한다")
    void issueRefreshToken_MissingIdToken_ThrowsUnauthorized() {
        AppleIdentity expectedIdentity = new AppleIdentity(APPLE_USER_ID, null, BUNDLE_ID);

        bindMockServer().expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponseJson("apple-refresh-token", null), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> appleService.issueRefreshToken("auth-code", expectedIdentity, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("교환 응답에 refresh_token 이 없으면 거부한다")
    void issueRefreshToken_MissingRefreshToken_ThrowsUnauthorized() throws Exception {
        AppleIdentity expectedIdentity = new AppleIdentity(APPLE_USER_ID, null, BUNDLE_ID);
        String exchangeIdToken = identityToken(AppleServiceImpl.APPLE_ISSUER, BUNDLE_ID, Instant.now().plusSeconds(600));

        bindMockServer().expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponseJson(null, exchangeIdToken), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> appleService.issueRefreshToken("auth-code", expectedIdentity, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("교환 응답의 id_token 서명이 잘못되면 거부한다")
    void issueRefreshToken_InvalidIdTokenSignature_ThrowsUnauthorized() throws Exception {
        AppleIdentity expectedIdentity = new AppleIdentity(APPLE_USER_ID, null, BUNDLE_ID);
        // Apple 이 아닌 발급자 -> verifyIdentityToken 이 거부한다.
        String invalidIdToken = identityToken("https://evil.example", BUNDLE_ID, Instant.now().plusSeconds(600));

        bindMockServer().expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponseJson("apple-refresh-token", invalidIdToken), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> appleService.issueRefreshToken("auth-code", expectedIdentity, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("교환 응답 identity 의 sub 가 최초 identity 와 다르면 거부한다")
    void issueRefreshToken_SubMismatch_ThrowsUnauthorized() throws Exception {
        AppleIdentity expectedIdentity = new AppleIdentity(APPLE_USER_ID, null, BUNDLE_ID);
        String otherUserIdToken = identityToken(AppleServiceImpl.APPLE_ISSUER, BUNDLE_ID, "999999.other.user", Instant.now().plusSeconds(600));

        bindMockServer().expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponseJson("apple-refresh-token", otherUserIdToken), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> appleService.issueRefreshToken("auth-code", expectedIdentity, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("교환 응답 identity 의 clientId 가 최초 identity 와 다르면 거부한다")
    void issueRefreshToken_ClientIdMismatch_ThrowsUnauthorized() throws Exception {
        // 최초 identity 는 앱 번들 ID 로 로그인했지만, 교환 응답 id_token 은 웹 Services ID 로 발급된 경우.
        AppleIdentity expectedIdentity = new AppleIdentity(APPLE_USER_ID, null, BUNDLE_ID);
        String otherClientIdToken = identityToken(AppleServiceImpl.APPLE_ISSUER, SERVICES_ID, Instant.now().plusSeconds(600));

        bindMockServer().expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenResponseJson("apple-refresh-token", otherClientIdToken), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> appleService.issueRefreshToken("auth-code", expectedIdentity, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("refresh token·clientId 로 직접 revoke 하는 메서드는 Apple 호출이 실패하면 503 을 반환한다")
    void revoke_ByRefreshTokenAndClientId_AppleApiFails_ThrowsServiceUnavailable() {
        bindMockServer().expect(requestTo(REVOKE_URI))
                .andRespond(withServerError());

        assertThatThrownBy(() -> appleService.revoke("apple-refresh-token", BUNDLE_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
