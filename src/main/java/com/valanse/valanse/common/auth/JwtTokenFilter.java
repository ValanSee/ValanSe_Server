package com.valanse.valanse.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtTokenFilter extends GenericFilter {
    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String uri = request.getRequestURI();
//        System.out.println("uri = " + uri);
        if (
                uri.equals("/auth/kakao/login") ||
                uri.equals("/auth/reissue") ||
                uri.equals("/error") ||
                uri.equals("/health") ||
                uri.startsWith("/swagger-ui") ||
                uri.equals("/swagger-ui.html") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-resources") ||
                uri.startsWith("/webjars"))
        {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);

                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();

                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));

                UserDetails userDetails = new User(claims.getSubject(), "", authorities);
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, jwtToken, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);

                // << 프로젝트 전역에서 사용하는 방법 >>
                // userDetails의 claims.getSubject() 꺼내기 (여기서는 이메일): SecurityContextHolder.getContext().getAuthentication().getName();
                // jwtToken 꺼내기: SecurityContextHolder.getContext().getAuthentication().getCredentials();
                // userDetails의 권한값(userDetails.getAuthorities()) 꺼내기: SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            } else {

                throw new AuthenticationServiceException("Token is not valid");
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401
            response.setContentType("application/json");
            response.getWriter().write("invalid token");
        }
    }
}

