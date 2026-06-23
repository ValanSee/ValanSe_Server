package com.valanse.valanse.service.AuthService;

import com.valanse.valanse.common.auth.JwtTokenProvider;
import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.Role;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.service.RefreshTokenService.RefreshTokenServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenServiceImpl refreshTokenService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("refresh token 재발급 시 DB의 현재 role을 새 access token에 반영한다")
    void reissueAccessToken_usesCurrentMemberRole() {
        String refreshToken = "refresh-token";
        Member member = Member.builder()
                .id(1L)
                .role(Role.ADMIN)
                .build();

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn("1");
        when(refreshTokenService.getRefreshToken("1")).thenReturn(refreshToken);
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(1L, Role.ADMIN.toString())).thenReturn("new-access-token");

        Map<String, String> result = authService.reissueAccessToken(refreshToken);

        assertThat(result.get("accessToken")).isEqualTo("new-access-token");
        verify(jwtTokenProvider).createAccessToken(1L, Role.ADMIN.toString());
    }

    @Test
    @DisplayName("관리자 subject 0 refresh token은 기존 정책대로 ADMIN role을 발급한다")
    void reissueAccessToken_adminSubjectZeroKeepsAdminRole() {
        String refreshToken = "admin-refresh-token";

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn("0");
        when(refreshTokenService.getRefreshToken("0")).thenReturn(refreshToken);
        when(jwtTokenProvider.createAccessToken(0L, Role.ADMIN.toString())).thenReturn("admin-access-token");

        Map<String, String> result = authService.reissueAccessToken(refreshToken);

        assertThat(result.get("accessToken")).isEqualTo("admin-access-token");
        verify(memberRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(jwtTokenProvider).createAccessToken(0L, Role.ADMIN.toString());
    }
}
