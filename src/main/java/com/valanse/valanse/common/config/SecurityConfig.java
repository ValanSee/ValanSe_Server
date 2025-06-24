package com.valanse.valanse.common.config;

import com.valanse.valanse.common.auth.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;

    // HTTP 요청이 들어올때 JWT토큰이 유효한지 확인하는 필터
    //유효하다면 SecurityContextHolder에 인증 객체를 등록해서, 인증된 사용자로 인식됨.
    public SecurityConfig(JwtTokenFilter jwtTokenFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
    }

    // 사용자 비밀번호를 해싱(암호화)하고, 비교하는 데 사용하는 PasswordEncoder 빈 등록
    // 기본적으로 bcrypt 알고리즘을 사용하며, 회원가입/로그인 시 비밀번호 처리에 사용됨
    @Bean
    public PasswordEncoder makePassword() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // 플젝 전체에 대한 시큐리티 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(a -> a.requestMatchers(
                        "/auth/kakao/login",
                        "/auth/reissue",
                        "/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**"
                ).permitAll().anyRequest().authenticated()) // 위 경로들은 인증 없이 접근 허용, 나머지는 JWT 토큰 필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CORS 정책 적용 (프론트와 연동 허용)
                .csrf(AbstractHttpConfigurer::disable)
                // CSRF 보호 비활성화 (JWT 방식은 불필요)
                .httpBasic(AbstractHttpConfigurer::disable)
                // HTTP Basic 인증 비활성화 (JWT 방식 사용)
                .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 세션 사용하지 않음 (Stateless 방식)
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 커스텀 JWT 필터 등록
                .build();
                 // 최종 SecurityFilterChain 생성
    }

    // cors 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedOrigins(Arrays.asList("https://valanse-sooty.vercel.app/"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

