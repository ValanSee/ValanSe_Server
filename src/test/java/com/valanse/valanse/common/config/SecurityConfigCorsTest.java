package com.valanse.valanse.common.config;

import com.valanse.valanse.common.auth.JwtTokenFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    @DisplayName("prod profile에서는 운영 도메인만 CORS origin으로 허용한다")
    void corsConfiguration_ProdProfile_AllowsOnlyProductionOrigins() {
        CorsConfiguration configuration = corsConfiguration("prod");

        assertThat(configuration.checkOrigin("https://valanse.kr")).isEqualTo("https://valanse.kr");
        assertThat(configuration.checkOrigin("https://www.valanse.kr")).isEqualTo("https://www.valanse.kr");
        assertThat(configuration.checkOrigin(
                "https://valanse-origin-repo-git-feature-125onb-97e2be-emithens-projects.vercel.app"
        )).isNull();
        assertThat(configuration.checkOrigin("https://develop.valanse.kr")).isNull();
    }

    @Test
    @DisplayName("dev profile에서는 localhost, 개발 도메인, ValanSe 프리뷰만 허용한다")
    void corsConfiguration_DevProfile_AllowsOnlyValanseDevelopmentOrigins() {
        CorsConfiguration configuration = corsConfiguration("dev");

        assertThat(configuration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configuration.checkOrigin("https://develop.valanse.kr")).isEqualTo("https://develop.valanse.kr");
        assertThat(configuration.checkOrigin(
                "https://valanse-origin-repo-git-feature-125onb-97e2be-emithens-projects.vercel.app"
        )).isEqualTo(
                "https://valanse-origin-repo-git-feature-125onb-97e2be-emithens-projects.vercel.app"
        );
        assertThat(configuration.checkOrigin("https://attacker-project.vercel.app")).isNull();
        assertThat(configuration.checkOrigin("https://test-front-security.netlify.app")).isNull();
    }

    @Test
    @DisplayName("CORS 응답에서 traceId 헤더를 클라이언트에 노출한다")
    void corsConfiguration_ExposesTraceIdHeader() {
        CorsConfiguration configuration = corsConfiguration("prod");

        assertThat(configuration.getExposedHeaders()).containsExactly("X-Trace-Id");
    }

    private CorsConfiguration corsConfiguration(String activeProfile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfile);

        SecurityConfig securityConfig = new SecurityConfig(mock(JwtTokenFilter.class), environment);
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/votes");
        return source.getCorsConfiguration(request);
    }
}
