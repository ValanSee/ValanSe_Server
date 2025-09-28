package com.valanse.valanse.dto.Vote;

import com.valanse.valanse.domain.enums.VoteCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class VoteDetailResponse {
    private Long voteId;
    private String title;
    private String content; // 투표 상세 내용
    private VoteCategory category;
    private Integer totalVoteCount;
    private String creatorNickname; // 생성자의 닉네임을 원한다고 가정
    private LocalDateTime createdAt;
    private List<VoteOptionDto> options; // 투표 옵션을 위한 DTO
    // --- 추가될 필드 ---
    private Boolean hasVoted; // 사용자가 이 투표에 투표했는지 여부
    private String votedOptionLabel; // 사용자가 투표한 선택지의 라벨 (예: "A", "B")
    // --- 추가될 필드 끝 ---

    @Getter
    @Builder
    public static class VoteOptionDto {
        private Long optionId;
        private String content;
        private Integer voteCount;
        private String label;
    }
}