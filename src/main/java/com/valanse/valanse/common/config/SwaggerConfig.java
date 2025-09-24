package com.valanse.valanse.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        servers = {
                @Server(url = "http://backendbase.store:8080", description = "운영 서버 (prod)"),
                @Server(url = "http://backendbase.store:8081", description = "개발 서버 (dev)"),
                @Server(url = "https://backendbase.store", description = "HTTPS 배포 서버")
     }
)
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String jwtSchemeName = "bearerAuth";

        return new OpenAPI()
                // API 기본 정보 설정
                .info(new Info()
                        .title("Valanse API Docs")
                        .version("v1") // API 버전
                        .description("카카오 OAuth 로그인은 <strong>프론트엔드에서 먼저 진행</strong>해 주세요.<br><br>" +
                                "프론트에서 로그인 성공 시 발급받은 <strong>access token</strong>을 Swagger의 <strong>Authorize 버튼</strong>에 입력해주시면 됩니다!<br>" +
                                "이때, <strong>Bearer 없이</strong> 토큰 값만 입력해 주세요.<br><br>" )
                )
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
