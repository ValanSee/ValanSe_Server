package com.valanse.valanse.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String jwtSchemeName = "bearerAuth"; // 인증 방식 이름 지정

        return new OpenAPI()
                // API 기본 정보 설정
                .info(new Info()
                        .title("Valanse API Docs") // 문서 제목
                        .version("v1") // API 버전
                        .description("OAuth 로그인 요청 URL:<br>" +
                                "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=dfb1600c00bc8120aee6d3abceeeac85&redirect_uri=https://valan-se-web.vercel.app/oauth/kakao/redirect<br><br>" +
                                "🔑 Authorize 버튼에 토큰 입력 시 <strong>Bearer 없이</strong> 토큰 값만 넣어주시면 됩니다!") // Swagger 문서 내 설명
                )
                //.addServersItem(new Server().url("http://localhost:8080"))
                .addServersItem(new Server().url("https://valanse-server.com"))
                // 전역 보안 설정: 모든 API 호출 시 JWT 토큰 필요하도록 설정
                .addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))
                // JWT 인증 스키마 설정
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName,
                                new SecurityScheme()
                                        .name(jwtSchemeName) // 스키마 이름
                                        .type(SecurityScheme.Type.HTTP) // HTTP 방식 인증
                                        .scheme("bearer") // Bearer 방식 사용
                                        .bearerFormat("JWT"))); // 토큰 형식은 JWT


    }
}
