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
     */
    @Override
    public String issueRefreshToken(String authorizationCode, String clientId, String redirectUri) {
        ensureConfigured();
        if (!StringUtils.hasText(authorizationCode)) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_ISSUE_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }

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
            response = RestClient.create().post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_ISSUE_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }

        Object refreshToken = response == null ? null : response.get("refresh_token");
        if (!(refreshToken instanceof String token) || token.isBlank()) {
            throw new ApiException(AuthErrorMessage.APPLE_TOKEN_ISSUE_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }
        return token;
    }

    /**
     * 회원의 Apple refresh token 을 폐기해 앱과 Apple ID 연결을 끊는 메서드입니다. (App Store 심사 5.1.1(v) 요구사항)
     */
    @Override
    public void revoke(Member member) {
        if (!StringUtils.hasText(member.getAppleRefreshToken()) || !StringUtils.hasText(member.getAppleClientId())) {
            throw new ApiException(AuthErrorMessage.APPLE_REFRESH_TOKEN_NOT_FOUND.message(), HttpStatus.BAD_REQUEST);
        }
        ensureConfigured();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", member.getAppleClientId());
        params.add("client_secret", createClientSecret(member.getAppleClientId()));
        params.add("token", member.getAppleRefreshToken());
        params.add("token_type_hint", "refresh_token");

        RestClient.create().post()
                .uri(REVOKE_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .toBodilessEntity();
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
