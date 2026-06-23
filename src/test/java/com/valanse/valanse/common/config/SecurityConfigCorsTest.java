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
        assertThat(configuration.checkOrigin("https://feature-preview.vercel.app")).isNull();
        assertThat(configuration.checkOrigin("https://test-front-security.netlify.app")).isNull();
    }

    @Test
    @DisplayName("local profile에서는 preview와 localhost origin을 허용한다")
    void corsConfiguration_LocalProfile_AllowsDevelopmentOrigins() {
        CorsConfiguration configuration = corsConfiguration("local");

        assertThat(configuration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configuration.checkOrigin("https://feature-preview.vercel.app")).isEqualTo("https://feature-preview.vercel.app");
        assertThat(configuration.checkOrigin("https://test-front-security.netlify.app")).isEqualTo("https://test-front-security.netlify.app");
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
