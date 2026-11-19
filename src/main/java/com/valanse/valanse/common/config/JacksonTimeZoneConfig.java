package com.valanse.valanse.common.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 모든 API 응답의 LocalDateTime 필드에 한국 시간(+09:00) 오프셋을 일관되게 적용합니다.
 * 날짜만 의미하는 필드(LocalDate 타입)는 영향받지 않습니다.
 */
@Configuration
public class JacksonTimeZoneConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer koreaTimeZoneCustomizer() {
        return builder -> builder.serializerByType(LocalDateTime.class, new KoreaTimeZoneSerializer());
    }
}
