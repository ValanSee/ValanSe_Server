package com.valanse.valanse.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                                "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=dfb1600c00bc8120aee6d3abceeeac85&redirect_uri=http://localhost:3000/oauth/kakao/redirect<br><br>" +
                                "🔑 토큰 입력 시 <strong>Bearer 없이</strong> 토큰 값만 넣어주시면 됩니다!")
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
