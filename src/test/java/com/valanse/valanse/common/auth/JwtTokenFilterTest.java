package com.valanse.valanse.common.auth;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenFilterTest {

    @Test
    @DisplayName("잘못된 JWT 형식은 기존과 같이 401 응답을 반환한다")
    void doFilter_MalformedJwt_ReturnsUnauthorized() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        JwtTokenFilter filter = new JwtTokenFilter(tokenProvider);
        MockHttpServletRequest request = requestWithBearerToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(tokenProvider.parseClaims("token"))
                .thenThrow(new MalformedJwtException("malformed"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 처리 중 예상하지 못한 내부 예외는 상위 Filter로 전파한다")
    void doFilter_UnexpectedException_Propagates() {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        JwtTokenFilter filter = new JwtTokenFilter(tokenProvider);
        MockHttpServletRequest request = requestWithBearerToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        IllegalStateException exception = new IllegalStateException("internal failure");
        when(tokenProvider.parseClaims("token")).thenThrow(exception);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isSameAs(exception);
    }

    private MockHttpServletRequest requestWithBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/member/profile");
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
