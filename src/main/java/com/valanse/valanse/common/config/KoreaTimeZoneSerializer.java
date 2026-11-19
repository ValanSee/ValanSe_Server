package com.valanse.valanse.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * DB에 저장된 LocalDateTime(타임존 정보 없음)을 UTC 시각으로 간주하고,
 * API 응답 경계에서만 Asia/Seoul(+09:00)로 변환해 ISO-8601 오프셋 포함 문자열로 직렬화합니다.
 * DB에 저장된 값 자체는 이 직렬화기로 바뀌지 않습니다.
 */
public class KoreaTimeZoneSerializer extends StdSerializer<LocalDateTime> {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    public KoreaTimeZoneSerializer() {
        super(LocalDateTime.class);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String formatted = value.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(KOREA_ZONE)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        gen.writeString(formatted);
    }
}
