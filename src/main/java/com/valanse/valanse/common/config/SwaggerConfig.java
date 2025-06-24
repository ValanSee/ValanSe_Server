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
        final String jwtSchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Valanse API Docs")
                        .version("v1")
                        .description("OAuth 로그인 요청 URL:<br>" +
                                "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=dfb1600c00bc8120aee6d3abceeeac85&redirect_uri=https://valanse-sooty.vercel.app/oauth/kakao/redirect<br><br>" +
                                "🔑 Authorize 버튼에 토큰 입력 시 <strong>Bearer 없이</strong> 토큰 값만 넣어주시면 됩니다!")
                )
                .addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName,
                                new SecurityScheme()
                                        .name(jwtSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));

    }
}
