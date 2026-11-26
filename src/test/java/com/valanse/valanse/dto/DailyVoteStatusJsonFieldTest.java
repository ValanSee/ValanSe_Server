package com.valanse.valanse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valanse.valanse.dto.DailyVote.DailyVoteReward;
import com.valanse.valanse.dto.DailyVote.DailyVoteStatus;
import com.valanse.valanse.dto.DailyVote.DailyVoteTally;
import com.valanse.valanse.dto.Vote.VoteCancleResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 투표 응답에 추가된 daily 블록의 JSON 키(특히 aCount 가 acount 로 바뀌지 않는지)와
 * 기존 최상위 키가 그대로인지 직렬화 결과로 확인합니다.
 */
class DailyVoteStatusJsonFieldTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("VoteCancleResponseDto는 daily 키와 기존 최상위 키로 직렬화된다")
    void VoteCancleResponseDto는_daily_키와_기존_키로_직렬화된다() throws Exception {
        VoteCancleResponseDto dto = new VoteCancleResponseDto(true, 10, 7L, 5);
        dto.setDaily(new DailyVoteStatus(new DailyVoteReward(3, false), new DailyVoteTally(5, 4)));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.at("/daily/reward/point").asInt()).isEqualTo(3);
        assertThat(json.at("/daily/reward/alreadyRewarded").isBoolean()).isTrue();
        assertThat(json.at("/daily/reward/alreadyRewarded").asBoolean()).isFalse();
        assertThat(json.at("/daily/todayTally/aCount").asLong()).isEqualTo(5);
        assertThat(json.at("/daily/todayTally/bCount").asLong()).isEqualTo(4);
        assertThat(json.at("/daily/todayTally").has("acount")).isFalse();
        assertThat(json.has("voted")).isTrue();
        assertThat(json.get("totalVoteCount").asInt()).isEqualTo(10);
        assertThat(json.get("voteOptionId").asLong()).isEqualTo(7L);
        assertThat(json.get("voteOptionCount").asInt()).isEqualTo(5);
    }
}
