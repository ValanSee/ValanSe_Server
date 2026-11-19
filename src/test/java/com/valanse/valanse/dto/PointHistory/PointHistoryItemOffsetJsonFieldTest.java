package com.valanse.valanse.dto.PointHistory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.valanse.valanse.common.config.KoreaTimeZoneSerializer;
import com.valanse.valanse.domain.enums.PointType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #344: PointHistoryItem.createdAt이 미리 포맷된 문자열이 아니라 LocalDateTime
 * 그대로 내려가, 전역 직렬화기가 적용되어 +09:00 오프셋을 포함하는지 확인합니다.
 */
class PointHistoryItemOffsetJsonFieldTest {

    private final ObjectMapper objectMapper = buildObjectMapper();

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new KoreaTimeZoneSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Test
    void PointHistoryItem의_createdAt은_날짜와_시각과_오프셋을_모두_포함한다() throws Exception {
        PointHistoryResponse.PointHistoryItem item = new PointHistoryResponse.PointHistoryItem(
                1L,
                40L,
                40L,
                PointType.SIGN_UP,
                "회원가입",
                LocalDateTime.of(2026, 10, 25, 19, 0, 0)
        );

        String json = objectMapper.writeValueAsString(item);

        assertThat(json).contains("\"createdAt\":\"2026-10-26T04:00:00+09:00\"");
    }
}
