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
    private String secretKey; // application.yml에서 설정된 JWT 비밀 키를 주입받음

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String uri = request.getRequestURI();
        // System.out.println("uri = " + uri);
        // 인증 없이 접근 가능한 URI 목록 (토큰 검사 생략)
        if (
                uri.equals("/auth/kakao/login") ||
                        uri.equals("/auth/reissue") ||
                        uri.equals("/error") ||
                        uri.equals("/health") ||
                        uri.startsWith("/swagger-ui") ||
                        uri.equals("/swagger-ui.html") ||
                        uri.startsWith("/v3/api-docs") ||
                        uri.startsWith("/swagger-resources") ||
                        uri.startsWith("/webjars") ||

                        // GET /votes/{voteId}/comments 허용
                        (uri.matches("^/votes/\\d+/comments$") && request.getMethod().equals("GET")) ||

                        // GET /votes/{voteId}/comments/{commentId}/replies 허용
                        (uri.matches("^/votes/\\d+/comments/\\d+/replies$") && request.getMethod().equals("GET")) ||

                        // GET /votes/{voteId}/comments/best 허용
                        (uri.matches("^/votes/\\d+/comments/best$") && request.getMethod().equals("GET"))
        ) {
            filterChain.doFilter(request, response);
            return;
        }


        // Authorization 헤더에서 토큰 추출
        String token = request.getHeader("Authorization");
        try {
            // Bearer로 시작하는 토큰인지 확인
            if (token != null && token.startsWith("Bearer ")) {
                // "Bearer " 접두어 제거
                String jwtToken = token.substring(7);
                // 토큰 파싱 및 검증
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey) // 비밀키 설정
                        .build()
                        .parseClaimsJws(jwtToken) // JWT 파싱
                        .getBody(); // claim(페이로드) 부분 가져오기
                // 권한 정보 생성 (예: ROLE_USER)
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));
                // 인증 객체 생성
                UserDetails userDetails = new User(claims.getSubject(), "", authorities);
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, jwtToken, userDetails.getAuthorities());
                // SecurityContext에 등록 → 이후 컨트롤러 등에서 인증 정보 사용 가능
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

