package com.valanse.valanse.dto.Vote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.valanse.valanse.common.config.KoreaTimeZoneSerializer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #344: TrendingVotesResponse의 from·to는 DB에 저장된 UTC 시각이 아니라
 * clock(Asia/Seoul)으로 만든 한국 시간이므로, 전역 LocalDateTime 직렬화기가 또 9시간을
 * 더하면 안 됩니다. OffsetDateTime으로 내려가 오프셋만 붙고 숫자는 그대로여야 합니다.
 */
class TrendingVotesResponseOffsetJsonFieldTest {

    private final ObjectMapper objectMapper = buildObjectMapper();

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new KoreaTimeZoneSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Test
    void from_to는_이미_한국시간이므로_전역_직렬화기가_추가로_9시간을_더하지_않는다() throws Exception {
        LocalDateTime koreaWallClock = LocalDateTime.of(2026, 11, 19, 16, 30, 0);
        OffsetDateTime from = koreaWallClock.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();

        TrendingVotesResponse response = TrendingVotesResponse.builder()
                .requestedDays(7)
                .from(from)
                .to(from)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"from\":\"2026-11-19T16:30:00+09:00\"");
        assertThat(json).doesNotContain("2026-11-20T01:30:00");
    }
}
