package com.valanse.valanse.dto.VotesCheck;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * VoteGenderResultResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class VoteGenderResultResponseDto {

    private Long voteId;            // 투표 ID
    private String gender;          // "F"
    private int totalCount;         // 여성 유저 총 투표 수
    private List<OptionResultDto> options; // 선택지별 결과

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionResultDto {
        private String label;       // A, B
        private String content;     // 선택지 텍스트
        private int voteCount;      // 해당 선택지 투표 수
        private float ratio;        // 비율 (%)
    }
}