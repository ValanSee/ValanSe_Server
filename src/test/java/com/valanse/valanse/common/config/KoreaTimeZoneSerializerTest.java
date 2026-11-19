package com.valanse.valanse.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KoreaTimeZoneSerializerTest {

    private final ObjectMapper objectMapper = buildObjectMapper();

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new KoreaTimeZoneSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Test
    void UTC로_저장된_시각이_날짜가_바뀌는_한국시간_오프셋으로_직렬화된다() throws Exception {
        LocalDateTime utcStoredValue = LocalDateTime.of(2026, 10, 25, 19, 0, 0);

        String json = objectMapper.writeValueAsString(utcStoredValue);

        assertThat(json).isEqualTo("\"2026-10-26T04:00:00+09:00\"");
    }

    @Test
    void 날짜가_바뀌지_않는_시각도_오프셋이_정확히_붙는다() throws Exception {
        LocalDateTime utcStoredValue = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        String json = objectMapper.writeValueAsString(utcStoredValue);

        assertThat(json).isEqualTo("\"2026-01-01T09:00:00+09:00\"");
    }
}
