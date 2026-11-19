package com.valanse.valanse.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JacksonTimeZoneConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 스프링이_주입하는_ObjectMapper도_LocalDateTime에_한국시간_오프셋을_적용한다() throws Exception {
        LocalDateTime utcStoredValue = LocalDateTime.of(2026, 10, 25, 19, 0, 0);

        String json = objectMapper.writeValueAsString(utcStoredValue);

        assertThat(json).isEqualTo("\"2026-10-26T04:00:00+09:00\"");
    }
}
