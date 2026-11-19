package com.valanse.valanse.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.valanse.valanse.common.config.KoreaTimeZoneSerializer;
import com.valanse.valanse.domain.Vote;
import com.valanse.valanse.domain.enums.PinType;
import com.valanse.valanse.domain.enums.VoteCategory;
import com.valanse.valanse.dto.Report.ReportedVoteResponse;
import com.valanse.valanse.dto.Vote.VoteListResponse;
import com.valanse.valanse.dto.Vote.VoteResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #344: /votes 목록을 비롯한 투표 관련 응답의 createdAt이 날짜로만 잘리지 않고
 * 날짜·시각·+09:00 오프셋을 모두 포함해 직렬화되는지 확인합니다.
 */
class CreatedAtOffsetJsonFieldTest {

    private final ObjectMapper objectMapper = buildObjectMapper();

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new KoreaTimeZoneSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Test
    void VoteResponseDto의_createdAt은_날짜만_아니라_시각과_오프셋도_포함한다() throws Exception {
        String json = objectMapper.writeValueAsString(new VoteResponseDto(voteWithCreatedAt()));

        assertThat(json).contains("\"createdAt\":\"2026-10-26T04:00:00+09:00\"");
    }

    @Test
    void VoteListResponse_VoteDto의_created_at은_날짜만_아니라_시각과_오프셋도_포함한다() throws Exception {
        VoteListResponse.VoteDto voteDto = VoteListResponse.VoteDto.builder()
                .id(1L)
                .created_at(LocalDateTime.of(2026, 10, 25, 19, 0, 0))
                .build();

        String json = objectMapper.writeValueAsString(voteDto);

        assertThat(json).contains("\"created_at\":\"2026-10-26T04:00:00+09:00\"");
    }

    @Test
    void ReportedVoteResponse의_createdAt은_날짜만_아니라_시각과_오프셋도_포함한다() throws Exception {
        String json = objectMapper.writeValueAsString(new ReportedVoteResponse(voteWithCreatedAt()));

        assertThat(json).contains("\"createdAt\":\"2026-10-26T04:00:00+09:00\"");
    }

    private Vote voteWithCreatedAt() {
        Vote vote = Vote.builder()
                .id(1L)
                .title("title")
                .content("content")
                .category(VoteCategory.ALL)
                .totalVoteCount(0)
                .pinType(PinType.NONE)
                .build();
        ReflectionTestUtils.setField(vote, "createdAt", LocalDateTime.of(2026, 10, 25, 19, 0, 0));
        return vote;
    }
}
