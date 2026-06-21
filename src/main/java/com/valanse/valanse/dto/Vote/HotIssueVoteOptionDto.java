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
/**
 * HotIssueVoteOptionDto API 응답 또는 계층 간 전달 값을 담는 DTO 코드입니다.
 */
public class HotIssueVoteOptionDto {
    private Long optionId; // 선택지의 ID
    private String content; // 투표 옵션 내용
    private Integer vote_count; // 해당 옵션의 투표 수
}