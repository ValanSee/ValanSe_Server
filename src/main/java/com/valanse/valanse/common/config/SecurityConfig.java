package com.valanse.valanse.common.config;

import com.valanse.valanse.common.auth.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

    public SecurityConfig(JwtTokenFilter jwtTokenFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
    }

    @Bean
    public PasswordEncoder makePassword() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // Preflight (CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 공개 API
                        .requestMatchers(
                                "/auth/kakao/login",
                                "/auth/reissue",
                                "/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // ============================================
                        // HTTP 메서드별 설정 (중요! 순서 지키기!)
                        // ============================================

                        // GET - 공개 (모든 사람이 볼 수 있음)
                        .requestMatchers(HttpMethod.GET, "/votes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/comments/**").permitAll()

                        // POST - 인증 필요 (로그인해야 가능)
                        .requestMatchers(HttpMethod.POST, "/votes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/votes/*/vote-options/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/votes/*/comments").authenticated()
                        .requestMatchers(HttpMethod.POST, "/comments/*/like").authenticated()

                        // PUT - 인증 필요 (수정)
                        .requestMatchers(HttpMethod.PUT, "/votes/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/comments/*").authenticated()

                        // DELETE - 인증 필요 (삭제)
                        .requestMatchers(HttpMethod.DELETE, "/votes/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/comments/*").authenticated()

                        // 특별한 경로 (내가 만든/투표한 게임)
                        .requestMatchers("/votes/mine/**").authenticated()

                        // 회원 관련
                        .requestMatchers("/member/**").authenticated()

                        // 나머지 모든 요청
                        .anyRequest().authenticated()
                )
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://test-front-security.netlify.app",
                "https://valan-se-web.vercel.app",
                "https://valanse.kr",
                "https://develop.valanse.kr",
                "https://backendbase.store",
                "http://backendbase.store:8080",
                "http://backendbase.store:8081",
                "http://backendbase.store:8082",
                "https://backendbase.store:8080",
                "https://backendbase.store:8081",
                "https://backendbase.store:8082"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}