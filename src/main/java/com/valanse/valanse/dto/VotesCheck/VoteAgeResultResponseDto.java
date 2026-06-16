package com.valanse.valanse.dto.VotesCheck;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * VoteAgeResultResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class VoteAgeResultResponseDto {
    private Long voteId;
    private Map<String, AgeGroupStats> ageRatios; // A, B -> 선택지별 통계

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AgeGroupStats {
        private int totalCount;
        private Map<String, AgeRatioDto> ageGroups; // 10대, 20대 등 -> 통계
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AgeRatioDto {
        private String content;
        private int voteCount;
        private float ratio;
    }
}
