package com.valanse.valanse.dto.VotesCheck;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
