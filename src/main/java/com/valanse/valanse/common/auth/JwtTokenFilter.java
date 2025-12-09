package com.valanse.valanse.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 요청이 컨트롤러에 도착하기 전에 JWT 유효성을 검사하는 필터
 *
 * 동작:
 * 1. Authorization 헤더에서 JWT 토큰 추출
 * 2. 토큰 유효성 검증
 * 3. 유효하면 SecurityContext에 인증 정보 설정
 * 4. 만료되거나 유효하지 않으면 401 반환 (프론트엔드 자동 갱신 트리거)
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * 이 필터를 건너뛸 요청들
     *
     * 주의: SecurityConfig의 permitAll()과는 다름!
     * - shouldNotFilter: 토큰 검사 자체를 안 함
     * - SecurityConfig permitAll: 토큰 검사는 하되, 인증 없어도 통과
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // CORS Preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 인증 관련 엔드포인트 (토큰 검사하면 순환 참조!)
        return uri.equals("/auth/kakao/login") ||
                uri.equals("/auth/reissue") ||
                uri.equals("/error") ||
                uri.equals("/health") ||
                // Swagger
                uri.startsWith("/swagger-ui") ||
                uri.equals("/swagger-ui.html") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-resources") ||
<<<<<<< HEAD
                uri.startsWith("/webjars") ;
//                uri.equals("/votes/best") ||
//                (uri.equals("/votes") && "GET".equalsIgnoreCase(request.getMethod())) ||
//                (uri.matches("^/votes/\\d+/comments$") && "GET".equalsIgnoreCase(request.getMethod())) ||
//                (uri.matches("^/votes/\\d+/comments/\\d+/replies$") && "GET".equalsIgnoreCase(request.getMethod())) ||
//                (uri.matches("^/votes/\\d+/comments/best$") && "GET".equalsIgnoreCase(request.getMethod()));
=======
                uri.startsWith("/webjars");
>>>>>>> dev
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String bearer = request.getHeader("Authorization");

        // ============================================
        // 토큰이 있을 때만 검증 시도
        // ============================================
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            String jwtToken = bearer.substring(7);

            try {
                // 토큰 파싱 및 검증
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();

                // 권한 정보 추출
                List<GrantedAuthority> authorities = new ArrayList<>();
                Object role = claims.get("role");
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }

                // 인증 객체 생성 및 SecurityContext에 설정
                UserDetails userDetails = new User(claims.getSubject(), "", authorities);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                userDetails, jwtToken, userDetails.getAuthorities()
                        )
                );

            } catch (ExpiredJwtException e) {
                // ============================================
                // ✅ 만료된 토큰: 401 반환
                // ============================================
                // 프론트엔드 Axios Interceptor가 401을 감지하고
                // 자동으로 /auth/reissue 호출하여 토큰 갱신
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"EXPIRED_TOKEN\",\"message\":\"토큰이 만료되었습니다.\"}");
                return; // 필터 체인 중단!

            } catch (io.jsonwebtoken.security.SecurityException |
                     io.jsonwebtoken.MalformedJwtException e) {
                // ============================================
                // ✅ 유효하지 않은 토큰: 401 반환
                // ============================================
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"유효하지 않은 토큰입니다.\"}");
                return;

            } catch (Exception e) {
                // ============================================
                // ✅ 기타 예외: 401 반환
                // ============================================
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"TOKEN_ERROR\",\"message\":\"토큰 처리 중 오류가 발생했습니다.\"}");
                return;
            }
        }

        // ============================================
        // 다음 필터로 진행
        // ============================================
        // - 토큰이 없거나
        // - 토큰이 유효해서 인증 설정 완료
        filterChain.doFilter(request, response);
    }
}