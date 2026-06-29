package com.valanse.valanse.dto.Vote;// src/main/java/com/valanse/valanse/dto/vote/VoteCreateRequest.java


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.valanse.valanse.domain.enums.VoteCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * VoteCreateRequest API 요청 값을 전달하는 DTO 코드입니다.
 */
public class VoteCreateRequest {
    private String title;
    private String content; // 투표 상세 내용 (선택사항)
    private List<OptionRequest> options; // 최대 4개 옵션을 받을 수 있도록 정의
    private VoteCategory category; // ERD의 Vote 테이블 category와 매핑

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionRequest {
        private String key;
        private String content;
        private String imageKey;

        /**
         * 구형 문자열 옵션과 이미지 업로드용 객체 옵션을 모두 지원합니다.
         */
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public OptionRequest(JsonNode value) {
            if (value.isTextual()) {
                this.content = value.textValue();
                return;
            }

            if (!value.isObject()) {
                throw new IllegalArgumentException("투표 옵션은 문자열 또는 객체여야 합니다.");
            }

            this.key = nullableText(value, "key");
            this.content = nullableText(value, "content");
            this.imageKey = nullableText(value, "imageKey");
        }

        private static String nullableText(JsonNode object, String fieldName) {
            JsonNode field = object.get(fieldName);
            return field == null || field.isNull() ? null : field.asText();
        }
    }
}
