package com.valanse.valanse.common.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(AnthropicProperties.class)
public class AnthropicConfig {

    // API Key가 비어 있어도 애플리케이션이 기동돼야 하므로, 실제 키가 없으면
    // 형식상의 placeholder를 사용한다. 이 경우 실제 API 호출은 인증에 실패한다.
    private static final String PLACEHOLDER_API_KEY = "not-configured";

    @Bean
    public AnthropicClient anthropicClient(AnthropicProperties properties) {
        String apiKey = StringUtils.hasText(properties.getApiKey())
                ? properties.getApiKey()
                : PLACEHOLDER_API_KEY;

        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
