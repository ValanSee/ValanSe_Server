// src/main/java/com/valanse/valanse/dto/Vote/HotIssueVoteOptionDto.java
package com.valanse.valanse.dto.Vote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotIssueVoteOptionDto {
    private Long optionId; // 선택지의 ID
    private String content; // 투표 옵션 내용
    private Integer vote_count; // 해당 옵션의 투표 수
}