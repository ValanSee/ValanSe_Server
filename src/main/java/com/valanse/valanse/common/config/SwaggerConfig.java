package com.valanse.valanse.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//Swagger(현재는 OpenAPI)는 API 문서를 자동으로 생성해주는 도구입니다. 이 설정 파일은 API 문서의 제목, 버전, 설명 등을 정의하고,
//JWT 기반 인증을 위한 보안 스키마(bearerAuth)를 추가하여 Swagger UI에서 토큰을 쉽게 입력하고 테스트할 수 있도록 돕습니다.
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
                                "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=dfb1600c00bc8120aee6d3abceeeac85&redirect_uri=https://valanse-sooty.vercel.app/oauth/kakao/redirect<br><br>" +
                                "🔑 Authorize 버튼에 토큰 입력 시 <strong>Bearer 없이</strong> 토큰 값만 넣어주시면 됩니다!") // Swagger 문서 내 설명
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
