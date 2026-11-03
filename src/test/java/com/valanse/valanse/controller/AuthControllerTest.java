package com.valanse.valanse.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.valanse.valanse.domain.Member;
import com.valanse.valanse.domain.enums.SocialType;
import com.valanse.valanse.dto.Login.AccessTokenDto;
import com.valanse.valanse.dto.Login.AppleIdentity;
import com.valanse.valanse.dto.Login.KakaoProfileDto;
import com.valanse.valanse.repository.MemberRepository;
import com.valanse.valanse.service.AppleService.AppleService;
import com.valanse.valanse.service.KakaoService.KakaoServiceImpl;
import com.valanse.valanse.service.RefreshTokenService.RefreshTokenServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RefreshTokenServiceImpl refreshTokenService;

    @MockBean
    private AppleService appleService;

    @MockBean
    private KakaoServiceImpl kakaoService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("관리자 계정으로 로그인하면 ADMIN 토큰을 발급한다")
    void adminLogin_ValidCredentials_ReturnsAdminToken() throws Exception {
        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(0))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.accessToken").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken").value(not(blankOrNullString())));
    }

    @Test
    @DisplayName("관리자 로그인 정보가 틀리면 401을 반환한다")
    void adminLogin_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("처음 Apple 로그인하면 refresh token 을 교환해 APPLE 회원을 만들고 JWT 를 발급한다")
    void appleLogin_NewUser_CreatesAppleMember() throws Exception {
        String appleUserId = "001234.newuser.0001";
        AppleIdentity identity = new AppleIdentity(appleUserId, "new@privaterelay.appleid.com", "com.valanse");
        when(appleService.verifyIdentityToken("id-token")).thenReturn(identity);
        when(appleService.issueRefreshToken("auth-code", identity, null)).thenReturn("apple-refresh");

        mockMvc.perform(post("/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "id-token",
                                  "authorizationCode": "auth-code",
                                  "name": "홍길동"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.accessToken").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken").value(not(blankOrNullString())));

        Member member = memberRepository
                .findBySocialTypeAndSocialIdAndDeletedAtIsNull(SocialType.APPLE, appleUserId)
                .orElseThrow();
        assertThat(member.getEmail()).isEqualTo("new@privaterelay.appleid.com");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getAppleRefreshToken()).isEqualTo("apple-refresh");
        assertThat(member.getAppleClientId()).isEqualTo("com.valanse");
    }

    @Test
    @DisplayName("이미 가입한 Apple 회원은 code 교환 없이 기존 회원으로 로그인한다")
    void appleLogin_ExistingUser_SkipsCodeExchange() throws Exception {
        String appleUserId = "001234.existing.0002";
        Member saved = memberRepository.save(Member.builder()
                .socialType(SocialType.APPLE)
                .socialId(appleUserId)
                .appleRefreshToken("stored-refresh")
                .appleClientId("com.valanse")
                .build());
        when(appleService.verifyIdentityToken("id-token-2"))
                .thenReturn(new AppleIdentity(appleUserId, null, "com.valanse"));

        mockMvc.perform(post("/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "id-token-2",
                                  "authorizationCode": "auth-code-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()));

        verify(appleService, never()).issueRefreshToken(any(), any(), any());
    }

    @Test
    @DisplayName("같은 socialId를 가진 KAKAO 회원과 APPLE 회원은 각자의 공급자로만 로그인된다")
    void kakaoAndAppleLogin_SameSocialId_ResolveToDifferentMembers() throws Exception {
        String sharedSocialId = "shared-social-id-0001";
        Member kakaoMember = memberRepository.save(Member.builder()
                .socialType(SocialType.KAKAO)
                .socialId(sharedSocialId)
                .build());
        Member appleMember = memberRepository.save(Member.builder()
                .socialType(SocialType.APPLE)
                .socialId(sharedSocialId)
                .appleRefreshToken("stored-refresh")
                .appleClientId("com.valanse")
                .build());

        when(kakaoService.getAccessToken(any(), any()))
                .thenReturn(new AccessTokenDto("kakao-access", "kakao-refresh", "3600", "scope", null));
        KakaoProfileDto.Profile profile = new KakaoProfileDto.Profile("닉네임", null);
        KakaoProfileDto.KakaoAccount account = new KakaoProfileDto.KakaoAccount("kakao@example.com", profile);
        when(kakaoService.getKakaoProfile("kakao-access"))
                .thenReturn(new KakaoProfileDto(sharedSocialId, account));

        mockMvc.perform(post("/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "kakao-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(kakaoMember.getId()));

        when(appleService.verifyIdentityToken("apple-id-token"))
                .thenReturn(new AppleIdentity(sharedSocialId, null, "com.valanse"));

        mockMvc.perform(post("/auth/apple/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identityToken": "apple-id-token",
                                  "authorizationCode": "auth-code"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appleMember.getId()));

        assertThat(kakaoMember.getId()).isNotEqualTo(appleMember.getId());
        verify(appleService, never()).issueRefreshToken(any(), any(), any());
    }
}
