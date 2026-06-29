package com.valanse.valanse.dto.Vote;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoteCreateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 구형_문자열_옵션_배열을_역직렬화한다() throws Exception {
        String json = """
                {
                  "title": "가격 선택",
                  "content": "설명",
                  "options": ["500", "1000"],
                  "category": "BUY"
                }
                """;

        VoteCreateRequest request = objectMapper.readValue(json, VoteCreateRequest.class);

        assertThat(request.getOptions())
                .extracting(VoteCreateRequest.OptionRequest::getContent)
                .containsExactly("500", "1000");
        assertThat(request.getOptions())
                .extracting(VoteCreateRequest.OptionRequest::getImageKey)
                .containsOnlyNulls();
    }

    @Test
    void 이미지용_객체_옵션_배열을_역직렬화한다() throws Exception {
        String json = """
                {
                  "title": "이미지 선택",
                  "options": [
                    {
                      "key": "A",
                      "content": "첫 번째",
                      "imageKey": "option-a-image"
                    }
                  ],
                  "category": "ETC"
                }
                """;

        VoteCreateRequest request = objectMapper.readValue(json, VoteCreateRequest.class);
        VoteCreateRequest.OptionRequest option = request.getOptions().get(0);

        assertThat(option.getKey()).isEqualTo("A");
        assertThat(option.getContent()).isEqualTo("첫 번째");
        assertThat(option.getImageKey()).isEqualTo("option-a-image");
    }
}
