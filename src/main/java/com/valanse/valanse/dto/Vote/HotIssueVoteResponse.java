// src/main/java/com/valanse/valanse/dto/Vote/HotIssueVoteResponse.java
package com.valanse.valanse.dto.Vote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotIssueVoteResponse {
    private Long voteId; // 가장 투표 참여 횟수가 많은 투표의 id
    private String title; // 가장 투표 참여 횟수가 많은 투표의 제목
    private String content;
    private String category; // 가장 투표 참여 횟수가 많은 투표의 카테고리
    private Integer totalParticipants; // 가장 투표 참여 횟수가 많은 투표의 총 투표 수
    private String createdBy; // 가장 투표 참여 횟수가 많은 투표를 생성한 사람의 닉네임
    private LocalDateTime createdAt; // 투표 생성 날짜
    private List<HotIssueVoteOptionDto> options; // 투표 옵션 리스트 (content, vote_count)
}