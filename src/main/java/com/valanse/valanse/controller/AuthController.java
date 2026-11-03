package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.common.message.AuthErrorMessage;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.dto.Login.AccessTokenDto;
import com.valanse.valanse.dto.Login.AdminLoginRequest;
import com.valanse.valanse.dto.Login.AppleIdentity;
import com.valanse.valanse.dto.Login.AppleLoginRequest;
import com.valanse.valanse.dto.Login.KakaoProfileDto;
import com.valanse.valanse.dto.Login.RedirectDto;
import com.valanse.valanse.dto.Login.ReissueRequestDto;
import com.valanse.valanse.service.AppleService.AppleService;
import com.valanse.valanse.service.AuthService.AuthServiceImpl;
import com.valanse.valanse.service.KakaoService.KakaoServiceImpl;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.RefreshTokenService.RefreshTokenServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "로그인 API", description = "카카오·Apple 로그인, 로그아웃, 토큰 재발급 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
/**
 * 카카오·Apple 로그인, JWT 재발급, 로그아웃, 회원 탈퇴 요청을 처리하는 인증 컨트롤러 코드입니다.
 */
public class AuthController {

    private final RefreshTokenServiceImpl refreshTokenService;
    private final AuthServiceImpl authService;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoServiceImpl kakaoService;
    private final AppleService appleService;

    @Value("${jwt.refresh-token-expiration}")
    private int refreshTokenExpirationMinutes;

    @Value("${jwt.admin-access-token-expiration}")
    private int adminAccessTokenExpirationMinutes;

    @Value("${jwt.admin-refresh-token-expiration}")
    private int adminRefreshTokenExpirationMinutes;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin}")
    private String adminPassword;

    private long refreshTokenExpirationMillis;
    private long adminRefreshTokenExpirationMillis;

    /**
     * AuthController의 init 기능을 수행하는 메서드입니다.
     */
    @PostConstruct
    public void init() {
        this.refreshTokenExpirationMillis = refreshTokenExpirationMinutes * 60L * 1000L;
        this.adminRefreshTokenExpirationMillis = adminRefreshTokenExpirationMinutes * 60L * 1000L;
    }

    @PostMapping("/admin/login")
    @Operation(summary = "관리자 로그인", description = "CMS 관리자 계정으로 로그인하고 ADMIN 권한 JWT를 발급합니다.")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody AdminLoginRequest request) {
        if (!adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
            throw new ApiException(AuthErrorMessage.ADMIN_ONLY.message(), HttpStatus.UNAUTHORIZED);
        }

        Map<String, String> jwtToken = jwtTokenProvider.createTokenPair(
                0L,
                Role.ADMIN.toString(),
                adminAccessTokenExpirationMinutes,
                adminRefreshTokenExpirationMinutes
        );
        refreshTokenService.saveRefreshToken("0", jwtToken.get("refreshToken"), adminRefreshTokenExpirationMillis);

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", 0L);
        loginInfo.put("role", Role.ADMIN.toString());
        loginInfo.put("accessToken", jwtToken.get("accessToken"));
        loginInfo.put("refreshToken", jwtToken.get("refreshToken"));

        return ResponseEntity.ok(loginInfo);
    }

    @Operation(
            summary = "카카오 로그인",
            description = "프론트엔드에서 인가코드(code)를 받아서 카카오 로그인을 처리합니다. 만약 회원이 존재하지 않으면 자동 회원가입 로직 수행 후 JWT 토큰을 발급하여 반환합니다."
    )
    /**
     * 카카오 인가 코드를 JWT 로그인 응답으로 교환하는 메서드입니다.
     */
    @PostMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody RedirectDto redirectDto) {
        AccessTokenDto accessTokenDto = kakaoService.getAccessToken(
                redirectDto.getCode(),
                redirectDto.getRedirectUri()
        );

        if (accessTokenDto == null || accessTokenDto.getAccess_token() == null) {
            throw new ApiException(AuthErrorMessage.KAKAO_ACCESS_TOKEN_ISSUE_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }

        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(accessTokenDto.getAccess_token());
        if (kakaoProfileDto == null || kakaoProfileDto.getId() == null) {
            throw new ApiException(AuthErrorMessage.KAKAO_PROFILE_FETCH_FAILED.message(), HttpStatus.UNAUTHORIZED);
        }

        Member originalMember = memberService.getMemberBySocial(SocialType.KAKAO, kakaoProfileDto.getId());
        if (originalMember == null) {
            originalMember = memberService.createOauth(
                    kakaoProfileDto.getId(),
                    kakaoProfileDto.getKakao_account().getEmail(),
                    kakaoProfileDto.getKakao_account().getProfile().getNickname(),
                    kakaoProfileDto.getKakao_account().getProfile().getProfile_image_url(),
                    accessTokenDto.getAccess_token(),
                    accessTokenDto.getRefresh_token()
            );
        }

        return ResponseEntity.ok(createLoginInfo(originalMember));
    }

    @Operation(
            summary = "Apple 로그인",
            description = "Apple 로그인으로 받은 identityToken 을 검증해 로그인합니다. 회원이 없으면 authorizationCode 를 Apple refresh token(탈퇴 시 연결 해제용)으로 교환한 뒤 자동 회원가입합니다. "
                    + "iOS 앱은 redirectUri 없이, 웹(Apple JS)은 authorize 에 쓴 redirectUri 를 함께 보냅니다. 응답은 카카오 로그인과 같습니다."
    )
    /**
     * Apple identity token 을 JWT 로그인 응답으로 교환하는 메서드입니다.
     */
    @PostMapping("/apple/login")
    public ResponseEntity<?> appleLogin(@RequestBody AppleLoginRequest request) {
        AppleIdentity identity = appleService.verifyIdentityToken(request.getIdentityToken());

        Member member = memberService.getMemberBySocial(SocialType.APPLE, identity.userId());
        if (member == null) {
            String appleRefreshToken = appleService.issueRefreshToken(
                    request.getAuthorizationCode(),
                    identity,
                    request.getRedirectUri()
            );
            try {
                member = memberService.createAppleMember(
                        identity.userId(),
                        identity.email(),
                        request.getName(),
                        appleRefreshToken,
                        identity.clientId()
                );
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 같은 Apple 계정이 먼저 가입된 경우: 기존 활성 회원으로 로그인하고,
                // 이번 요청에서 발급받았지만 저장되지 못한 refresh token은 보상 revoke 한다.
                Member existing = memberService.getMemberBySocial(SocialType.APPLE, identity.userId());
                revokeOrphanAppleRefreshToken(appleRefreshToken, identity.clientId());
                if (existing == null) {
                    throw e; // 활성 회원이 없다면 중복 충돌이 아닌 실제 DB 오류이므로 그대로 전파한다.
                }
                member = existing;
            } catch (RuntimeException e) {
                // 중복 충돌이 아닌 일반 저장 실패에도, 발급받은 refresh token은 보상 revoke 한다.
                revokeOrphanAppleRefreshToken(appleRefreshToken, identity.clientId());
                throw e;
            }
        }

        return ResponseEntity.ok(createLoginInfo(member));
    }

    /**
     * Apple 회원 저장이 실패해 이번 요청에서 발급된 refresh token 이 버려질 때 Apple 쪽에도 폐기를 시도한다.
     * 보상 revoke 실패는 로그만 남기고 무시한다 (원래 저장 오류를 덮어쓰지 않기 위함). 토큰 원문은 로그에 남기지 않는다.
     */
    private void revokeOrphanAppleRefreshToken(String appleRefreshToken, String clientId) {
        try {
            appleService.revoke(appleRefreshToken, clientId);
        } catch (Exception revokeFailure) {
            System.out.println("Apple 보상 revoke 실패 (무시): " + revokeFailure.getMessage());
        }
    }

    /**
     * 회원 JWT 를 발급하고 refresh token 을 저장한 로그인 응답을 만드는 메서드입니다.
     */
    private Map<String, Object> createLoginInfo(Member member) {
        Map<String, String> jwtToken = jwtTokenProvider.createTokenPair(
                member.getId(),
                member.getRole().toString()
        );

        // jwt refresh token 저장
        refreshTokenService.saveRefreshToken(
                member.getId().toString(),
                jwtToken.get("refreshToken"),
                refreshTokenExpirationMillis
        );

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", member.getId());
        loginInfo.put("accessToken", jwtToken.get("accessToken"));
        loginInfo.put("refreshToken", jwtToken.get("refreshToken"));
        return loginInfo;
    }

    /**
     * 현재 access token의 subject 기준으로 Redis refresh token을 삭제하는 로그아웃 메서드입니다.
     */
    @Operation(summary = "로그아웃", description = "refresh token을 삭제하여 로그아웃 처리합니다. 이후부터는 기존의 refresh token으로 reissue를 시도하여도 새로운 access token 발급이 불가합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "access token 재발급",
            description = "만료된 access token 대신 refresh token을 사용해 새로운 access token을 발급합니다. "
                    + "refresh token이 유효하지 않거나 저장된 값과 다르면 401 에러를 반환합니다."
    )
    /**
     * AuthController의 reissueToken 기능을 수행하는 메서드입니다.
     */
    @PostMapping("/reissue")
    public ResponseEntity<Map<String, String>> reissueToken(@RequestBody ReissueRequestDto request) {
        String refreshToken = request.getRefreshToken();
        Map<String, String> tokenPair = authService.reissueAccessToken(refreshToken);
        return ResponseEntity.ok(tokenPair);
    }

    @Operation(
            summary = "회원 탈퇴 처리",
            description = "refresh token을 redis로부터 삭제해서 토큰 재발급 방지, 카카오·Apple 로그인 연결끊기, 그리고 사용자 데이터베이스 삭제를 진행합니다."
    )
    /**
     * 소셜 연결 해제, refresh token 삭제, 회원 소프트 삭제를 수행하는 탈퇴 메서드입니다.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw() {
        Long userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
        Member member = memberService.findById(userId);

        if (member.getSocialType() == SocialType.APPLE) {
            // Apple은 revoke가 성공해야만 탈퇴를 진행한다. 실패 시 ApiException(503)이 그대로 전파되어
            // 회원·Apple credential·Redis refresh token이 모두 보존되고, 이후 재시도가 가능하다.
            appleService.revoke(member);
        } else {
            // 카카오는 기존 정책 그대로 연결 끊기 실패를 무시하고 탈퇴를 계속 진행한다.
            try {
                kakaoService.unLink();
            } catch (Exception e) {
                System.out.println("카카오 연결 끊기 실패 (무시): " + e.getMessage());
            }
        }

        authService.logout(); // refresh token을 redis로부터 삭제
        memberService.deleteMemberById(); // 사용자 데이터베이스 내용 삭제
        return ResponseEntity.ok().build();
    }

}
