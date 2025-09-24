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
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;

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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // ← preflight 허용
                        .requestMatchers(
                                "/auth/kakao/login",
                                "/auth/reissue",
                                "/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/votes/best",
                                "/votes/*/comments/*/replies",
                                "/votes/*/comments/best"
                        ).permitAll()
                        // 추가된 부분: GET /votes 경로를 permitAll 허용
                        .requestMatchers(HttpMethod.GET, "/votes").permitAll() //
//                        .requestMatchers(HttpMethod.GET, "/votes/{voteId}").permitAll() // 새롭게 추가된 부분
                        .requestMatchers(HttpMethod.GET, "/votes/*/comments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/votes/*/comments").authenticated()
                        .anyRequest().authenticated()
                )
                .cors(c -> c.configurationSource(corsConfigurationSource())) // ← CORS 활성화
                //.cors(AbstractHttpConfigurer::disable) // CORS 비활성화
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
                "https://backendbase.store",
                "http://backendbase.store:8080",   // 운영 환경 HTTP
                "http://backendbase.store:8081",   // 개발 환경 HTTP ← 이거 추가!
                "https://backendbase.store:8080",  // 운영 환경 HTTPS (필요시)
                "https://backendbase.store:8081"   // 개발 환경 HTTPS (필요시)
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true); // 쿠키 및 JWT 등 인증 필요 시 true 설정

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
