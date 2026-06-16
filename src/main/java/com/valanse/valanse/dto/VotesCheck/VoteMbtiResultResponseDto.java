package com.valanse.valanse.dto.VotesCheck;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * VoteMbtiResultResponseDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class VoteMbtiResultResponseDto {
    private Long vote_id;
    private String mbti_type;
    private Integer total_count;
    private Map<String, List<OptionRatio>> mbti_ratios;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptionRatio {
        private String content;
        private int vote_count;
        private float ratio;


    }
}
