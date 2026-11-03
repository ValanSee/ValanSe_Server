package com.valanse.valanse.controller;

import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.dto.Login.AppleIdentity;
import com.valanse.valanse.dto.Login.AppleLoginRequest;
import com.valanse.valanse.service.AppleService.AppleService;
import com.valanse.valanse.service.AuthService.AuthServiceImpl;
import com.valanse.valanse.service.KakaoService.KakaoServiceImpl;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.RefreshTokenService.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Apple 신규 가입 중 DB 저장이 실패했을 때(동시 가입 충돌 / 일반 DB 오류) AuthController가
 * 올바른 회원으로 로그인시키고, 저장되지 못한 Apple refresh token을 보상 revoke 하는지 검증한다.
 * MemberService/AppleService를 모두 모킹한 순수 단위 테스트라서 실제 DB의 unique 제약(V11)과는
 * 별도로 컨트롤러의 분기 로직만 다룬다. (실제 unique 제약 동작은 ActiveSocialIdentityMigrationTest에서 검증)
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerAppleConflictTest {

    @Mock
    private RefreshTokenServiceImpl refreshTokenService;
    @Mock
    private AuthServiceImpl authService;
    @Mock
    private MemberService memberService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private KakaoServiceImpl kakaoService;
    @Mock
    private AppleService appleService;

    @InjectMocks
    private AuthController authController;

    private AppleLoginRequest request;
    private AppleIdentity identity;

    @BeforeEach
    void setUp() {
        request = new AppleLoginRequest("id-token", "auth-code", null, null);
        identity = new AppleIdentity("001234.apple.user", "user@privaterelay.appleid.com", "com.valanse");
        when(appleService.verifyIdentityToken("id-token")).thenReturn(identity);
    }

    @Test
    @DisplayName("동시 가입 충돌 시 기존 활성 회원으로 로그인하고, 저장되지 못한 refresh token은 보상 revoke 한다")
    void appleLogin_ConcurrentConflict_UsesExistingMemberAndRevokesOrphanToken() {
        Member existing = Member.builder()
                .id(10L).role(Role.USER).socialType(SocialType.APPLE)
                .socialId(identity.userId())
                .appleRefreshToken("winner-refresh").appleClientId("com.valanse")
                .build();

        when(memberService.getMemberBySocial(SocialType.APPLE, identity.userId()))
                .thenReturn(null, existing);
        when(appleService.issueRefreshToken("auth-code", identity, null)).thenReturn("orphan-refresh");
        when(memberService.createAppleMember(identity.userId(), identity.email(), null, "orphan-refresh", identity.clientId()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(jwtTokenProvider.createTokenPair(10L, Role.USER.toString()))
                .thenReturn(Map.of("accessToken", "a", "refreshToken", "r"));

        ResponseEntity<?> response = authController.appleLogin(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("id")).isEqualTo(10L);
        verify(appleService).revoke("orphan-refresh", "com.valanse");
        verify(memberService, times(2)).getMemberBySocial(SocialType.APPLE, identity.userId());
    }

    @Test
    @DisplayName("충돌 후 재조회해도 활성 회원이 없으면 실제 DB 오류를 그대로 전파한다")
    void appleLogin_ConflictButNoExistingMember_RethrowsOriginalException() {
        // getMemberBySocial은 스텁하지 않는다 — 모킹된 메서드의 기본 반환값은 null이므로,
        // 최초 조회·충돌 후 재조회 모두 null을 반환해 "활성 회원 없음" 상황을 그대로 나타낸다.
        when(appleService.issueRefreshToken("auth-code", identity, null)).thenReturn("orphan-refresh");
        DataIntegrityViolationException original = new DataIntegrityViolationException("duplicate");
        when(memberService.createAppleMember(identity.userId(), identity.email(), null, "orphan-refresh", identity.clientId()))
                .thenThrow(original);

        assertThatThrownBy(() -> authController.appleLogin(request)).isSameAs(original);

        verify(appleService).revoke("orphan-refresh", "com.valanse");
    }

    @Test
    @DisplayName("일반 DB 저장 실패 시에도 보상 revoke를 호출하고 원래 오류를 전파한다")
    void appleLogin_GenericSaveFailure_RevokesOrphanTokenAndRethrows() {
        when(memberService.getMemberBySocial(SocialType.APPLE, identity.userId())).thenReturn(null);
        when(appleService.issueRefreshToken("auth-code", identity, null)).thenReturn("orphan-refresh");
        RuntimeException original = new RuntimeException("DB connection lost");
        when(memberService.createAppleMember(identity.userId(), identity.email(), null, "orphan-refresh", identity.clientId()))
                .thenThrow(original);

        assertThatThrownBy(() -> authController.appleLogin(request)).isSameAs(original);

        verify(appleService).revoke("orphan-refresh", "com.valanse");
    }

    @Test
    @DisplayName("보상 revoke 자체가 실패해도 원래 DB 오류를 덮어쓰지 않는다")
    void appleLogin_CompensatingRevokeFails_StillRethrowsOriginalException() {
        when(memberService.getMemberBySocial(SocialType.APPLE, identity.userId())).thenReturn(null);
        when(appleService.issueRefreshToken("auth-code", identity, null)).thenReturn("orphan-refresh");
        RuntimeException original = new RuntimeException("DB connection lost");
        when(memberService.createAppleMember(identity.userId(), identity.email(), null, "orphan-refresh", identity.clientId()))
                .thenThrow(original);
        doThrow(new RuntimeException("Apple revoke 실패"))
                .when(appleService).revoke("orphan-refresh", "com.valanse");

        assertThatThrownBy(() -> authController.appleLogin(request)).isSameAs(original);
    }
}
