package com.valanse.valanse.service.AppleService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.message.AuthErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.dto.Login.AppleIdentity;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * Apple identity token 검증, authorization code 교환, 연결 해제(revoke)를 처리하는 외부 API 연동 서비스 코드입니다.
 * https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api
 */
public class AppleServiceImpl implements AppleService {

    static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String JWK_SET_URI = APPLE_ISSUER + "/auth/keys";
    private static final String TOKEN_URI = APPLE_ISSUER + "/auth/token";
    private static final String REVOKE_URI = APPLE_ISSUER + "/auth/revoke";
    private static final long CLIENT_SECRET_TTL_MINUTES = 5;

    @Value("${oauth.apple.team-id:}")
    private String teamId;

    @Value("${oauth.apple.key-id:}")
    private String keyId;

    // .p8 파일 내용 (PEM). 한 줄로 넣을 때는 줄바꿈을 \n 으로
    @Value("${oauth.apple.private-key:}")
    private String privateKey;

    // 앱 번들 ID(com.valanse)와 웹 Services ID 를 쉼표로 구분
    @Value("${oauth.apple.client-ids:}")
    private String clientIds;

    @Value("${oauth.apple.allowed-redirect-uris:http://localhost:3000/oauth/apple/redirect,https://develop.valanse.kr/oauth/apple/redirect,https://valanse.kr/oauth/apple/redirect}")
    private String allowedRedirectUris;

    private volatile JwtDecoder jwtDecoder;

    // 테스트에서 MockRestServiceServer로 교체할 수 있도록 필드로 둔다.
    private RestClient restClient = RestClient.create();

    /**
     * Apple 공개키(JWKS)로 identity token 의 서명·발급자·대상·만료를 검증하는 메서드입니다.
     */
    @Override
    public AppleIdentity verifyIdentityToken(String identityToken) {
        ensureConfigured();
        if (!StringUtils.hasText(identityToken)) {
            throw new ApiException(AuthErrorMessage.APPLE_IDENTITY_TOKEN_INVALID.message(), HttpStatus.UNAUTHORIZED);
        }

        Jwt jwt;
        try {
            jwt = getJwtDecoder().decode(identityToken);
        } catch (JwtException e) {
            throw new ApiException(AuthErrorMessage.APPLE_IDENTITY_TOKEN_INVALID.message(), HttpStatus.UNAUTHORIZED);
        }

        Set<String> allowedClientIds = splitCsv(clientIds);
        String clientId = jwt.getAudience().stream()
                .filter(allowedClientIds::contains)
                .findFirst()
                .orElseThrow(() -> new ApiException(AuthErrorMessage.APPLE_IDENTITY_TOKEN_INVALID.message(), HttpStatus.UNAUTHORIZED));

        return new AppleIdentity(jwt.getSubject(), jwt.getClaimAsString("email"), clientId);
    }

    /**
     * authorization code 를 Apple refresh token 으로 교환하는 메서드입니다. (탈퇴 시 revoke 에 필요)
     * 웹에서 받은 code 는 authorize 단계와 같은 redirect_uri 를 함께 보내야 합니다.
     * 교환 응답의 id_token 을 검증해 expectedIdentity(최초 identity token 검증 결과)와 같은 사용자(sub)·같은 clientId(aud)인지 대조한 뒤에만 refresh token 을 반환한다.
     */
    @Override
    public String issueRefreshToken(String authorizationCode, AppleIdentity expectedIdentity, String redirectUri) {
        ensureConfigured();
        if (!StringUtils.hasText(authorizationCode)) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_ISSUE_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }

        String clientId = expectedIdentity.clientId();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", createClientSecret(clientId));
        params.add("code", authorizationCode);
        params.add("grant_type", "authorization_code");
        if (StringUtils.hasText(redirectUri)) {
            params.add("redirect_uri", resolveRedirectUri(redirectUri));
        }

        Map<?, ?> response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_ISSUE_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }

        Object refreshTokenValue = response == null ? null : response.get("refresh_token");
        if (!(refreshTokenValue instanceof String refreshToken) || refreshToken.isBlank()) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_ISSUE_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }

        Object idTokenValue = response.get("id_token");
        if (!(idTokenValue instanceof String idToken) || idToken.isBlank()) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_EXCHANGE_IDENTITY_MISMATCH.message(), HttpStatus.UNAUTHORIZED);
        }

        // 서명·발급자·만료·aud 는 identity token 과 동일한 검증 로직을 그대로 재사용한다.
        AppleIdentity exchangedIdentity = verifyIdentityToken(idToken);
        if (!exchangedIdentity.userId().equals(expectedIdentity.userId())
                || !exchangedIdentity.clientId().equals(expectedIdentity.clientId())) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_EXCHANGE_IDENTITY_MISMATCH.message(), HttpStatus.UNAUTHORIZED);
        }

        return refreshToken;
    }

    /**
     * 회원의 Apple refresh token 을 폐기해 앱과 Apple ID 연결을 끊는 메서드입니다. (App Store 심사 5.1.1(v) 요구사항)
     * credential(refresh token·clientId) 자체가 없으면 revoke 할 대상이 없다는 뜻이므로 조용히 통과한다 —
     * 데이터 이상 등으로 이런 회원이라도 탈퇴(데이터 삭제)는 막지 않고, Apple 쪽 연결 해제는 사용자가
     * Apple 설정(Apple ID > 암호 및 보안 > 이 앱 연결 해제)에서 직접 하도록 남겨둔다.
     */
    @Override
    public void revoke(Member member) {
        if (!StringUtils.hasText(member.getAppleRefreshToken()) || !StringUtils.hasText(member.getAppleClientId())) {
            return;
        }
        revoke(member.getAppleRefreshToken(), member.getAppleClientId());
    }

    /**
     * 회원 엔티티 없이 refresh token·clientId 만으로 revoke 한다. (회원 저장 실패 시 보상 revoke 용도)
     * Apple 쪽 호출이 실패하면 일반 500 이 아니라 503(APPLE_REVOKE_FAILED) 으로 변환해, 재시도 가능한 상태임을 명확히 한다.
     */
    @Override
    public void revoke(String refreshToken, String clientId) {
        if (!StringUtils.hasText(refreshToken) || !StringUtils.hasText(clientId)) {
            throw new ApiException(AuthErrorMessage.APPLE_REFRESH_TOKEN_NOT_FOUND.message(), HttpStatus.BAD_REQUEST);
        }
        ensureConfigured();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", createClientSecret(clientId));
        params.add("token", refreshToken);
        params.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                    .uri(REVOKE_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ApiException(AuthErrorMessage.APPLE_REVOKE_FAILED.message(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Apple REST API 호출용 client secret(ES256 JWT)을 만드는 메서드입니다.
     */
    String createClientSecret(String clientId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setIssuer(teamId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(CLIENT_SECRET_TTL_MINUTES, ChronoUnit.MINUTES)))
                .setAudience(APPLE_ISSUER)
                .setSubject(clientId)
                .signWith(parsePrivateKey(privateKey), io.jsonwebtoken.SignatureAlgorithm.ES256)
                .compact();
    }

    String resolveRedirectUri(String requestedRedirectUri) {
        String redirectUri = requestedRedirectUri.trim();
        if (!splitCsv(allowedRedirectUris).contains(redirectUri)) {
            throw new ApiException(AuthErrorMessage.APPLE_REDIRECT_URI_NOT_ALLOWED.message(), HttpStatus.BAD_REQUEST);
        }
        return redirectUri;
    }

    /**
     * Apple identity token 검증 규칙: 발급자 appleid.apple.com, 만료, aud 가 우리 client_id 중 하나.
     */
    static OAuth2TokenValidator<Jwt> tokenValidator(Set<String> allowedClientIds) {
        JwtClaimValidator<List<String>> audienceValidator = new JwtClaimValidator<>(
                JwtClaimNames.AUD,
                aud -> aud != null && aud.stream().anyMatch(allowedClientIds::contains)
        );
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(APPLE_ISSUER),
                audienceValidator
        );
    }

    private JwtDecoder getJwtDecoder() {
        if (jwtDecoder == null) {
            synchronized (this) {
                if (jwtDecoder == null) {
                    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI)
                            .jwsAlgorithm(SignatureAlgorithm.RS256)
                            .build();
                    decoder.setJwtValidator(tokenValidator(splitCsv(clientIds)));
                    jwtDecoder = decoder;
                }
            }
        }
        return jwtDecoder;
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(teamId) || !StringUtils.hasText(keyId)
                || !StringUtils.hasText(privateKey) || splitCsv(clientIds).isEmpty()) {
            throw new ApiException(AuthErrorMessage.APPLE_LOGIN_NOT_CONFIGURED.message(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static PrivateKey parsePrivateKey(String pem) {
        String base64 = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new ApiException(AuthErrorMessage.APPLE_LOGIN_NOT_CONFIGURED.message(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static Set<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }
}
