package com.valanse.valanse.common.config;

import com.valanse.valanse.common.auth.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
import java.util.List;

@Configuration
/**
 * Spring Security 인가 규칙, CORS, JWT 필터 체인을 구성하는 보안 설정 코드입니다.
 */
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;
    private final Environment environment;
    // 테스트용 주석
    /**
     * SecurityConfig 의존성을 주입하거나 객체를 초기화하는 생성자입니다.
     */
    public SecurityConfig(JwtTokenFilter jwtTokenFilter, Environment environment) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.environment = environment;
    }

    /**
     * SecurityConfig의 makePassword 기능을 수행하는 메서드입니다.
     */
    @Bean
    public PasswordEncoder makePassword() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * SecurityConfig의 securityFilterChain 기능을 수행하는 메서드입니다.
     */
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

                        .requestMatchers(HttpMethod.POST, "/analytics/events/page-view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/analytics/mau").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/report").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/member/titles/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/member/titles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/member/titles/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/member/titles/*").hasRole("ADMIN")

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
                        .requestMatchers(HttpMethod.POST, "/storage/images").authenticated()

                        // PUT - 인증 필요 (수정)
                        .requestMatchers(HttpMethod.PUT, "/votes/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/comments/*").authenticated()

                        // DELETE - 인증 필요 (삭제)
                        .requestMatchers(HttpMethod.DELETE, "/votes/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/comments/*").authenticated()

                        // PATCH
                        .requestMatchers(HttpMethod.PATCH, "/votes/*/pin").hasRole("ADMIN")

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

    /**
     * SecurityConfig의 corsConfigurationSource 기능을 수행하는 메서드입니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins());
        configuration.setAllowedOriginPatterns(allowedOriginPatterns());

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/member/titles", configuration);
        source.registerCorsConfiguration("/member/titles/**", configuration);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> allowedOrigins() {
        if (isProdProfile()) {
            return List.of(
                    "https://valanse.kr",
                    "https://www.valanse.kr"
            );
        }

        return Arrays.asList(
                "http://localhost:3000",
                "https://valanse.kr",
                "https://www.valanse.kr",
                "https://develop.valanse.kr",
                "http://valanserver.store",
                "http://valanserver.store:8080",
                "http://valanserver.store:8081",
                "http://valanserver.store:8082",
                "https://valanserver.store:8080",
                "https://valanserver.store:8081",
                "https://valanserver.store:8082"
        );
    }

    private List<String> allowedOriginPatterns() {
        if (isProdProfile()) {
            return List.of(
                    "https://*.vercel.app",
                    "https://*.netlify.app"
            );
        }

        return Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.valanse.kr",
                "https://*.vercel.app",
                "https://*.netlify.app",
                "http://valanserver.store:[*]",
                "https://valanserver.store:[*]"
        );
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
