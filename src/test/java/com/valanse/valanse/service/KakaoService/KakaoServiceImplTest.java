package com.valanse.valanse.service.KakaoService;

import com.valanse.valanse.common.api.ApiException;
import com.valanse.valanse.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class KakaoServiceImplTest {

    private KakaoServiceImpl kakaoService;

    @BeforeEach
    void setUp() {
        kakaoService = new KakaoServiceImpl(mock(MemberRepository.class));
        ReflectionTestUtils.setField(kakaoService, "kakaoRedirectUri", "https://develop.valanse.kr/oauth/kakao/redirect");
        ReflectionTestUtils.setField(
                kakaoService,
                "kakaoAllowedRedirectUris",
                "http://localhost:3000/oauth/kakao/redirect,https://develop.valanse.kr/oauth/kakao/redirect"
        );
    }

    @Test
    @DisplayName("화이트리스트의 localhost redirect URI를 허용한다")
    void resolveRedirectUri_LocalhostRedirectUri_ReturnsRequestedUri() {
        String redirectUri = kakaoService.resolveRedirectUri("http://localhost:3000/oauth/kakao/redirect");

        assertThat(redirectUri).isEqualTo("http://localhost:3000/oauth/kakao/redirect");
    }

    @Test
    @DisplayName("redirect URI를 생략하면 환경의 기본값을 사용한다")
    void resolveRedirectUri_MissingRedirectUri_ReturnsDefaultUri() {
        String redirectUri = kakaoService.resolveRedirectUri(null);

        assertThat(redirectUri).isEqualTo("https://develop.valanse.kr/oauth/kakao/redirect");
    }

    @Test
    @DisplayName("화이트리스트에 없는 redirect URI는 카카오 요청 전에 거부한다")
    void getAccessToken_NotAllowedRedirectUri_ThrowsBadRequest() {
        assertThatThrownBy(() -> kakaoService.getAccessToken("code", "https://evil.example/callback"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("허용되지 않은 카카오 redirect URI입니다.");
                });
    }
}
