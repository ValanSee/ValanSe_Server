package com.valanse.valanse.controller;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.service.AppleService.AppleService;
import com.valanse.valanse.service.AuthService.AuthServiceImpl;
import com.valanse.valanse.service.KakaoService.KakaoServiceImpl;
import com.valanse.valanse.service.MemberService.MemberService;
import com.valanse.valanse.service.RefreshTokenService.RefreshTokenServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 탈퇴(/auth/withdraw) 시 Apple revoke 성공/실패에 따른 분기와, 카카오의 기존 best-effort 정책이
 * 바뀌지 않았는지를 검증하는 순수 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerWithdrawTest {

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

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", "dummy-jwt"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Apple revoke 성공 시 로그아웃과 회원 탈퇴를 진행한다")
    void withdraw_AppleRevokeSucceeds_ProceedsWithLogoutAndDelete() {
        Member member = Member.builder().id(1L).socialType(SocialType.APPLE)
                .appleRefreshToken("stored-refresh").appleClientId("com.valanse").build();
        when(memberService.findById(1L)).thenReturn(member);

        authController.withdraw();

        verify(appleService).revoke(member);
        verify(authService).logout();
        verify(memberService).deleteMemberById();
    }

    @Test
    @DisplayName("Apple revoke 실패 시 503을 전파하고, 로그아웃·탈퇴를 진행하지 않는다")
    void withdraw_AppleRevokeFails_PropagatesServiceUnavailableWithoutDeleting() {
        Member member = Member.builder().id(1L).socialType(SocialType.APPLE)
                .appleRefreshToken("stored-refresh").appleClientId("com.valanse").build();
        when(memberService.findById(1L)).thenReturn(member);
        doThrow(new ApiException("Apple 로그인 연결 해제에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE))
                .when(appleService).revoke(member);

        assertThatThrownBy(() -> authController.withdraw())
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        verify(authService, never()).logout();
        verify(memberService, never()).deleteMemberById();
    }

    @Test
    @DisplayName("카카오 unlink 실패는 기존처럼 무시하고 탈퇴를 계속 진행한다")
    void withdraw_KakaoUnlinkFails_StillProceedsWithLogoutAndDelete() {
        Member member = Member.builder().id(1L).socialType(SocialType.KAKAO).build();
        when(memberService.findById(1L)).thenReturn(member);
        doThrow(new RuntimeException("카카오 연결 끊기 실패")).when(kakaoService).unLink();

        authController.withdraw();

        verify(authService).logout();
        verify(memberService).deleteMemberById();
        verifyNoInteractions(appleService);
    }
}
