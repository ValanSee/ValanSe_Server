package com.valanse.valanse.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnhandledExceptionFilterTest {

    @Test
    @DisplayName("Filter Chain의 처리되지 않은 예외를 공통 예외 처리기로 전달한다")
    void doFilter_UnhandledException_DelegatesToResolver() throws Exception {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        UnhandledExceptionFilter filter = new UnhandledExceptionFilter(resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException exception = new IllegalStateException("boom");
        FilterChain chain = (req, res) -> {
            throw exception;
        };
        when(resolver.resolveException(request, response, null, exception))
                .thenReturn(new ModelAndView());

        filter.doFilter(request, response, chain);

        verify(resolver).resolveException(request, response, null, exception);
    }

    @Test
    @DisplayName("정상 요청은 공통 예외 처리기를 호출하지 않는다")
    void doFilter_NormalRequest_DoesNotUseResolver() throws Exception {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        UnhandledExceptionFilter filter = new UnhandledExceptionFilter(resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), isNull(), any());
    }

    @Test
    @DisplayName("공통 예외 처리기가 처리하지 못한 예외는 다시 전파한다")
    void doFilter_WhenResolverDoesNotHandle_RethrowsException() {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        UnhandledExceptionFilter filter = new UnhandledExceptionFilter(resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletException exception = new ServletException("boom");
        FilterChain chain = (req, res) -> {
            throw exception;
        };
        when(resolver.resolveException(request, response, null, exception)).thenReturn(null);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("응답이 이미 전송된 경우 예외 처리기를 호출하지 않고 원래 예외를 전파한다")
    void doFilter_CommittedResponse_RethrowsWithoutResolving() throws IOException {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        UnhandledExceptionFilter filter = new UnhandledExceptionFilter(resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stream");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);
        IllegalStateException exception = new IllegalStateException("boom");
        FilterChain chain = (req, res) -> {
            throw exception;
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isSameAs(exception);
        verify(resolver, never()).resolveException(any(), any(), isNull(), any());
    }
}
