package com.valanse.valanse.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring MVC에 도달하기 전에 Filter Chain에서 발생한 처리되지 않은 예외를
 * 공통 예외 처리기로 전달합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UnhandledExceptionFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver exceptionResolver;

    public UnhandledExceptionFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            if (response.isCommitted()) {
                rethrow(exception);
                return;
            }

            ModelAndView resolved = exceptionResolver.resolveException(
                    request,
                    response,
                    null,
                    exception
            );
            if (resolved == null) {
                rethrow(exception);
            }
        }
    }

    private void rethrow(Exception exception) throws ServletException, IOException {
        if (exception instanceof IOException ioException) {
            throw ioException;
        }
        if (exception instanceof ServletException servletException) {
            throw servletException;
        }
        if (exception instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new ServletException(exception);
    }
}
